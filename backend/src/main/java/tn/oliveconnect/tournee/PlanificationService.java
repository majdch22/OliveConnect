package tn.oliveconnect.tournee;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandeRecolteRepository;
import tn.oliveconnect.demande.StatutDemande;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.pressoir.CapacitePressoir;
import tn.oliveconnect.pressoir.CapacitePressoirRepository;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.referentiel.PressoirRepository;
import tn.oliveconnect.ressource.Equipe;
import tn.oliveconnect.ressource.EquipeRepository;
import tn.oliveconnect.ressource.EtatRessource;
import tn.oliveconnect.ressource.Ressource;
import tn.oliveconnect.ressource.RessourceRepository;
import tn.oliveconnect.ressource.TypeRessource;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;

/**
 * Planification d'une tournée par le responsable de collecte.
 *
 * Règles qui empêchent de créer la tournée (conflits, 409) :
 * - chaque demande choisie est encore « validée » ;
 * - une ressource n'est affectée qu'à une seule tournée par jour, et seulement si elle est disponible (ni en panne,
 *   ni en révision, ni en congé) ;
 * - un chef d'équipe n'a qu'une seule tournée par jour ;
 * - la date est aujourd'hui ou plus tard, dans la campagne de récolte.
 * Règles qui avertissent sans empêcher :
 * - la capacité des bennes affectées ne couvre pas la quantité estimée des vergers ;
 * - le pressoir n'a plus assez de capacité ce jour-là (d'autres pressoirs, les plus proches d'abord, sont proposés) ;
 * - pas de tracteur, pas d'ouvrier, ou date différente de la date souhaitée par l'agriculteur.
 * La capacité d'un pressoir ce jour-là est celle publiée par son responsable (sinon sa capacité habituelle) ;
 * la capacité réservée est la somme des quantités estimées des tournées du jour qui s'y rendent (agrégation MongoDB).
 */
@Service
@RequiredArgsConstructor
public class PlanificationService {

    static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);

    /** Un chef d'équipe libre ce jour-là. */
    public record ChefDisponible(String id, String nomComplet, String telephone) {
    }

    /** Une ressource disponible et libre ce jour-là. */
    public record RessourceDisponible(String id, String code, TypeRessource type, String libelleType, String nom,
            Integer capaciteKg, Integer quantite, String equipeId, String nomEquipe) {
    }

    /** Une équipe d'ouvriers : effectif et ouvriers affectables ce jour-là (« 7 sur 8 »), pour l'affecter en un clic. */
    public record EquipeDisponible(String id, String code, String nom, long effectif, List<String> ouvriersDisponibles) {
    }

    /** Un pressoir et sa capacité ce jour-là : maximum, déjà réservé par d'autres tournées, restant. */
    public record PressoirDisponible(String id, String code, String nom, String libelleDelegation, double latitude,
            double longitude, int capaciteJournaliereKg, int reserveKg, int restantKg) {
    }

    /** Ce qu'il est possible de choisir pour une tournée à cette date. */
    public record Options(LocalDate date, List<ChefDisponible> chefs, int chefsOccupes, List<RessourceDisponible> ressources,
            int ressourcesOccupees, List<EquipeDisponible> equipes, List<PressoirDisponible> pressoirs) {
    }

    /**
     * Résultat du contrôle d'une tournée avant sa création.
     *
     * @param conflits          problèmes qui empêchent de créer la tournée
     * @param avertissements    points à vérifier, qui n'empêchent pas la création
     * @param pressoirsSuggeres autres pressoirs qui ont encore la capacité nécessaire, du plus proche au plus éloigné
     */
    public record Verification(List<String> conflits, List<String> avertissements, int quantiteEstimeeKg,
            int capaciteBennesKg, PressoirDisponible pressoir, List<PressoirDisponible> pressoirsSuggeres) {

        public boolean possible() {
            return conflits.isEmpty();
        }
    }

    private final TourneeRepository tourneeRepository;
    private final DemandeRecolteRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RessourceRepository ressourceRepository;
    private final EquipeRepository equipeRepository;
    private final PressoirRepository pressoirRepository;
    private final CapacitePressoirRepository capaciteRepository;
    private final VergerRepository vergerRepository;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;
    private final Clock horloge;

    // --- Choix possibles ---

    public Options options(LocalDate date) {
        Occupation occupation = occupation(date);

        List<Utilisateur> chefs = utilisateurRepository.findByRoleAndStatut(Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        List<ChefDisponible> chefsLibres = chefs.stream()
                .filter(chef -> !occupation.chefs().containsKey(chef.getId()))
                .sorted(Comparator.comparing(Utilisateur::getNomComplet, String.CASE_INSENSITIVE_ORDER))
                .map(chef -> new ChefDisponible(chef.getId(), chef.getNomComplet(), chef.getTelephone()))
                .toList();

        Map<String, Equipe> equipes = equipeRepository.findAll().stream()
                .filter(equipe -> !equipe.isArchivee())
                .collect(Collectors.toMap(Equipe::getId, Function.identity()));
        List<Ressource> disponibles = mongoTemplate.find(Query.query(where("archivee").is(false)
                .and("etat").is(EtatRessource.DISPONIBLE)).with(Sort.by("type", "code")), Ressource.class);
        List<RessourceDisponible> ressourcesLibres = disponibles.stream()
                .filter(ressource -> !occupation.ressources().containsKey(ressource.getId()))
                .map(ressource -> {
                    Equipe equipe = ressource.getEquipeId() == null ? null : equipes.get(ressource.getEquipeId());
                    return new RessourceDisponible(ressource.getId(), ressource.getCode(), ressource.getType(),
                            ressource.getType().getLibelle(), ressource.getNom(), ressource.getCapaciteKg(),
                            ressource.getQuantite(), ressource.getEquipeId(), equipe == null ? null : equipe.getNom());
                })
                .toList();

        List<EquipeDisponible> equipesDisponibles = equipes.values().stream()
                .sorted(Comparator.comparing(Equipe::getCode))
                .map(equipe -> new EquipeDisponible(equipe.getId(), equipe.getCode(), equipe.getNom(),
                        ressourceRepository.countByEquipeIdAndArchiveeFalse(equipe.getId()),
                        ressourcesLibres.stream().filter(ressource -> equipe.getId().equals(ressource.equipeId()))
                                .map(RessourceDisponible::id).toList()))
                .toList();

        return new Options(date, chefsLibres, chefs.size() - chefsLibres.size(), ressourcesLibres,
                disponibles.size() - ressourcesLibres.size(), equipesDisponibles, pressoirs(date, occupation.reserveParPressoir()));
    }

    // --- Contrôle avant création ---

    public Verification verifier(PlanificationRequete requete) {
        verifierDate(requete.date());
        Occupation occupation = occupation(requete.date());
        List<String> conflits = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();

        // Demandes : elles doivent toutes être encore validées
        List<DemandeRecolte> demandes = demandesDans(requete);
        for (String demandeId : requete.demandeIds().stream().distinct().toList()) {
            Optional<DemandeRecolte> trouvee = demandes.stream().filter(demande -> demande.getId().equals(demandeId)).findFirst();
            if (trouvee.isEmpty()) {
                conflits.add("Une des demandes choisies n'existe plus.");
            } else if (trouvee.get().getStatut() != StatutDemande.VALIDEE) {
                conflits.add("La demande " + trouvee.get().getNumero() + " n'est plus validée (état : "
                        + trouvee.get().getStatut().getLibelle().toLowerCase(Locale.FRENCH) + ").");
            } else if (!trouvee.get().getDateSouhaitee().equals(requete.date())) {
                avertissements.add("La demande " + trouvee.get().getNumero() + " souhaitait le "
                        + trouvee.get().getDateSouhaitee().format(FORMAT_DATE) + " : l'agriculteur sera prévenu du changement de date.");
            }
        }

        // Chef d'équipe : un compte actif de ce rôle, sans autre tournée ce jour-là
        Utilisateur chef = chefEquipe(requete.chefEquipeId());
        String tourneeDuChef = occupation.chefs().get(chef.getId());
        if (tourneeDuChef != null) {
            conflits.add(chef.getNomComplet() + " a déjà la tournée " + tourneeDuChef
                    + " ce jour-là (une seule tournée par chef d'équipe et par jour).");
        }

        // Ressources : disponibles, non archivées, et pas déjà prises par une autre tournée ce jour-là
        Map<String, Ressource> ressources = ressourcesDans(requete);
        for (String ressourceId : requete.ressources()) {
            Ressource ressource = ressources.get(ressourceId);
            if (ressource == null || ressource.isArchivee()) {
                conflits.add("Une des ressources choisies n'existe plus ou a été archivée.");
            } else if (ressource.getEtat() != EtatRessource.DISPONIBLE) {
                conflits.add(ressource.getCode() + " (" + ressource.getNom() + ") est indisponible : "
                        + ressource.getEtat().getLibelle().toLowerCase(Locale.FRENCH) + ".");
            } else if (occupation.ressources().containsKey(ressourceId)) {
                conflits.add(ressource.getCode() + " est déjà affecté à la tournée " + occupation.ressources().get(ressourceId)
                        + " ce jour-là (une ressource par tournée et par jour).");
            }
        }

        // Capacité des bennes face à la quantité estimée
        int quantite = demandes.stream().mapToInt(DemandeRecolte::getQuantiteEstimeeKg).sum();
        List<Ressource> choisies = requete.ressources().stream().map(ressources::get).filter(ressource -> ressource != null).toList();
        int capaciteBennes = choisies.stream().filter(ressource -> ressource.getType() == TypeRessource.BENNE)
                .mapToInt(ressource -> ressource.getCapaciteKg() == null ? 0 : ressource.getCapaciteKg()).sum();
        if (capaciteBennes == 0) {
            avertissements.add("Aucune benne affectée : ajoutez au moins une benne pour transporter les olives.");
        } else if (capaciteBennes < quantite) {
            avertissements.add("Capacité des bennes " + kilos(capaciteBennes) + " < " + kilos(quantite)
                    + " estimés : ajoutez une benne.");
        }
        if (choisies.stream().noneMatch(ressource -> ressource.getType() == TypeRessource.TRACTEUR)) {
            avertissements.add("Aucun tracteur affecté.");
        }
        if (choisies.stream().noneMatch(ressource -> ressource.getType() == TypeRessource.OUVRIER)) {
            avertissements.add("Aucun ouvrier affecté.");
        }

        // Capacité du pressoir ce jour-là
        Pressoir pressoir = pressoir(requete.pressoirId());
        List<PressoirDisponible> pressoirs = pressoirs(requete.date(), occupation.reserveParPressoir());
        PressoirDisponible destination = pressoirs.stream().filter(candidat -> candidat.id().equals(pressoir.getId()))
                .findFirst().orElseThrow();
        List<PressoirDisponible> suggestions = List.of();
        if (quantite > destination.restantKg()) {
            avertissements.add(pressoir.getNom() + " n'a plus que " + kilos(Math.max(destination.restantKg(), 0))
                    + " de capacité ce jour-là pour " + kilos(quantite) + " estimés.");
            suggestions = pressoirsProches(demandes, pressoirs, quantite, pressoir.getId());
        }
        return new Verification(conflits, avertissements, quantite, capaciteBennes, destination, suggestions);
    }

    // --- Création ---

    public TourneeReponse creer(PlanificationRequete requete, String auteurId) {
        Verification verification = verifier(requete);
        if (!verification.possible()) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "PLANIFICATION_IMPOSSIBLE",
                    String.join(" ", verification.conflits()));
        }
        Utilisateur chef = chefEquipe(requete.chefEquipeId());
        Pressoir pressoir = pressoir(requete.pressoirId());
        Map<String, DemandeRecolte> demandes = demandesDans(requete).stream()
                .collect(Collectors.toMap(DemandeRecolte::getId, Function.identity()));
        Map<String, String> nomsAgriculteurs = utilisateurRepository.findAllById(
                        demandes.values().stream().map(DemandeRecolte::getAgriculteurId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Utilisateur::getNomComplet));
        Map<String, Ressource> ressources = ressourcesDans(requete);
        Map<String, String> nomsEquipes = equipeRepository.findAll().stream()
                .collect(Collectors.toMap(Equipe::getId, Equipe::getNom));

        Tournee tournee = new Tournee();
        tournee.setNumero(compteurService.prochainCode("TOURNEE", "T-%04d"));
        tournee.setDate(requete.date());
        tournee.setChefEquipeId(chef.getId());
        tournee.setNomChefEquipe(chef.getNomComplet());
        tournee.setPressoirId(pressoir.getId());
        tournee.setNomPressoir(pressoir.getNom());
        tournee.setCommentaire(requete.commentaire() == null || requete.commentaire().isBlank() ? null : requete.commentaire().trim());
        tournee.setCreateurId(auteurId);
        tournee.setDateCreation(horloge.instant());
        int ordre = 1;
        for (String demandeId : requete.demandeIds().stream().distinct().toList()) {
            DemandeRecolte demande = demandes.get(demandeId);
            tournee.getEtapes().add(new Tournee.Etape(ordre++, demande.getId(), demande.getNumero(), demande.getVergerId(),
                    demande.getNomVerger(), demande.getAgriculteurId(), nomsAgriculteurs.getOrDefault(demande.getAgriculteurId(), ""),
                    demande.getType(), demande.getQuantiteEstimeeKg()));
        }
        for (String ressourceId : requete.ressources()) {
            Ressource ressource = ressources.get(ressourceId);
            tournee.getAffectations().add(new Tournee.Affectation(ressource.getId(), ressource.getType(), ressource.getCode(),
                    ressource.getNom(), ressource.getType() == TypeRessource.BENNE ? ressource.getCapaciteKg() : null,
                    ressource.getEquipeId() == null ? null : nomsEquipes.get(ressource.getEquipeId())));
        }
        tournee.changerStatut(StatutTournee.PLANIFIEE, horloge.instant(), auteurId, null);
        Tournee enregistree = tourneeRepository.save(tournee);

        // Chaque demande passe « planifiée » ; la version de la demande protège d'une modification faite entre-temps
        for (Tournee.Etape etape : enregistree.getEtapes()) {
            DemandeRecolte demande = demandes.get(etape.demandeId());
            demande.setTourneeId(enregistree.getId());
            demande.setNumeroTournee(enregistree.getNumero());
            demande.setDatePlanifiee(enregistree.getDate());
            demande.changerStatut(StatutDemande.PLANIFIEE, horloge.instant(), auteurId,
                    "Tournée " + enregistree.getNumero() + " du " + enregistree.getDate().format(FORMAT_DATE));
            demandeRepository.save(demande);
            prevenirAgriculteur(demande, enregistree);
        }
        notificationService.notifierPersonne(chef.getId(), NouvelleNotification.simple(CategorieNotification.COLLECTE,
                "Nouvelle tournée " + enregistree.getNumero(),
                jour(enregistree.getDate()) + " · " + enregistree.getEtapes().size() + " verger(s) · "
                        + kilos(enregistree.quantiteEstimeeKg()) + " · pressoir " + enregistree.getNomPressoir(),
                "/chef-equipe"));
        return TourneeReponse.depuis(enregistree);
    }

    private void prevenirAgriculteur(DemandeRecolte demande, Tournee tournee) {
        String jour = jour(tournee.getDate());
        notificationService.notifierPersonne(demande.getAgriculteurId(), NouvelleNotification.simple(CategorieNotification.COLLECTE,
                        "Collecte planifiée · " + demande.getNomVerger(),
                        "Demande " + demande.getNumero() + " : l'équipe viendra le " + jour + " (tournée " + tournee.getNumero() + ").",
                        "/agriculteur/demandes")
                .avecSms("OliveConnect : la collecte de votre verger " + demande.getNomVerger() + " est planifiée le "
                        + tournee.getDate().format(FORMAT_DATE) + "."));
    }

    // --- Lecture de l'occupation du jour ---

    /**
     * Qui est déjà pris ce jour-là : ressources et chefs d'équipe (avec le numéro de la tournée qui les occupe),
     * et quantité déjà réservée dans chaque pressoir.
     */
    private record Occupation(Map<String, String> ressources, Map<String, String> chefs, Map<String, Integer> reserveParPressoir) {
    }

    private Occupation occupation(LocalDate date) {
        Map<String, String> ressources = new HashMap<>();
        Map<String, String> chefs = new HashMap<>();
        for (Tournee tournee : tourneeRepository.findByDateAndStatutIn(date, StatutTournee.ACTIVES)) {
            tournee.getAffectations().forEach(affectation -> ressources.put(affectation.ressourceId(), tournee.getNumero()));
            chefs.put(tournee.getChefEquipeId(), tournee.getNumero());
        }
        return new Occupation(ressources, chefs, reserveParPressoir(date));
    }

    /**
     * Agrégation MongoDB :
     * { $match: { date: <jour>, statut: { $in: [tournées actives] } } },
     * { $unwind: "$etapes" },
     * { $group: { _id: "$pressoirId", reserve: { $sum: "$etapes.quantiteEstimeeKg" } } }
     */
    Map<String, Integer> reserveParPressoir(LocalDate date) {
        TypedAggregation<Tournee> agregation = newAggregation(Tournee.class,
                match(where("date").is(date).and("statut").in(StatutTournee.ACTIVES)),
                unwind("etapes"),
                group("pressoirId").sum("etapes.quantiteEstimeeKg").as("reserve"));
        Map<String, Integer> reserve = new HashMap<>();
        for (Document ligne : mongoTemplate.aggregate(agregation, Document.class)) {
            reserve.put(ligne.getString("_id"), ((Number) ligne.get("reserve")).intValue());
        }
        return reserve;
    }

    /** Capacité de chaque pressoir ce jour-là : celle publiée par son responsable pour ce jour, sinon l'habituelle. */
    private List<PressoirDisponible> pressoirs(LocalDate date, Map<String, Integer> reserve) {
        Map<String, Integer> publiees = capaciteRepository.findByDate(date).stream()
                .collect(Collectors.toMap(CapacitePressoir::getPressoirId, CapacitePressoir::getMaximumKg));
        return pressoirRepository.findAll(Sort.by("nom")).stream()
                .filter(pressoir -> !pressoir.isArchive())
                .map(pressoir -> disponibilite(pressoir, publiees.getOrDefault(pressoir.getId(), pressoir.getCapaciteJournaliereKg()),
                        reserve.getOrDefault(pressoir.getId(), 0)))
                .toList();
    }

    private static PressoirDisponible disponibilite(Pressoir pressoir, int maximum, int reserve) {
        return new PressoirDisponible(pressoir.getId(), pressoir.getCode(), pressoir.getNom(),
                pressoir.getDelegation().getLibelle(), pressoir.getLocalisation().getY(), pressoir.getLocalisation().getX(),
                maximum, reserve, maximum - reserve);
    }

    /**
     * Pressoirs qui ont encore la capacité nécessaire, du plus proche au plus éloigné du premier verger de la tournée.
     * Requête MongoDB $nearSphere sur la position des pressoirs (index 2dsphere) : la base les renvoie déjà triés par distance.
     */
    private List<PressoirDisponible> pressoirsProches(List<DemandeRecolte> demandes, List<PressoirDisponible> pressoirs,
            int quantite, String pressoirChoisi) {
        Map<String, PressoirDisponible> parId = pressoirs.stream().collect(Collectors.toMap(PressoirDisponible::id, Function.identity()));
        Optional<GeoJsonPoint> depart = demandes.stream().findFirst()
                .flatMap(demande -> vergerRepository.findById(demande.getVergerId())).map(Verger::getLocalisation);
        List<String> ordre = depart.isEmpty() ? pressoirs.stream().map(PressoirDisponible::id).toList()
                : mongoTemplate.find(Query.query(where("localisation").nearSphere(depart.get()).and("archive").is(false)),
                        Pressoir.class).stream().map(Pressoir::getId).toList();
        return ordre.stream().map(parId::get)
                .filter(candidat -> candidat != null && !candidat.id().equals(pressoirChoisi) && candidat.restantKg() >= quantite)
                .limit(3).toList();
    }

    // --- Lectures et contrôles ---

    private void verifierDate(LocalDate date) {
        if (date.isBefore(LocalDate.now(horloge))) {
            throw erreurChamp("date", "La tournée ne peut pas être planifiée dans le passé.");
        }
        Parametres.Campagne campagne = parametresService.actuels().campagne();
        if (date.isBefore(campagne.debut()) || date.isAfter(campagne.fin())) {
            throw erreurChamp("date", "La date doit être comprise dans la campagne de récolte, du "
                    + campagne.debut().format(FORMAT_DATE) + " au " + campagne.fin().format(FORMAT_DATE) + ".");
        }
    }

    private List<DemandeRecolte> demandesDans(PlanificationRequete requete) {
        Map<String, DemandeRecolte> trouvees = new LinkedHashMap<>();
        demandeRepository.findAllById(requete.demandeIds()).forEach(demande -> trouvees.put(demande.getId(), demande));
        return List.copyOf(trouvees.values());
    }

    private Map<String, Ressource> ressourcesDans(PlanificationRequete requete) {
        Map<String, Ressource> trouvees = new HashMap<>();
        ressourceRepository.findAllById(requete.ressources()).forEach(ressource -> trouvees.put(ressource.getId(), ressource));
        return trouvees;
    }

    private Utilisateur chefEquipe(String id) {
        return utilisateurRepository.findById(id)
                .filter(compte -> compte.getRole() == Role.CHEF_EQUIPE && compte.estActif())
                .orElseThrow(() -> erreurChamp("chefEquipeId", "Choisissez un chef d'équipe actif."));
    }

    private Pressoir pressoir(String id) {
        return pressoirRepository.findById(id).filter(pressoir -> !pressoir.isArchive())
                .orElseThrow(() -> erreurChamp("pressoirId", "Choisissez un pressoir de la liste."));
    }

    static String kilos(int quantite) {
        return String.format(Locale.FRENCH, "%,d kg", quantite).replace(' ', ' ').replace(' ', ' ');
    }

    static String jour(LocalDate date) {
        return date.format(FORMAT_JOUR) + " " + date.getYear();
    }

    private static ErreurMetierException erreurChamp(String champ, String message) {
        return new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES", message, Map.of(champ, message));
    }
}
