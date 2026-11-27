package tn.oliveconnect.pressoir;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandeRecolteRepository;
import tn.oliveconnect.demande.StatutDemande;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.referentiel.CategorieQualite;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.ressource.TypeRessource;
import tn.oliveconnect.tournee.StatutTournee;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.tournee.TourneeRepository;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Réception des bennes au pressoir (UC50).
 *
 * Règles :
 * - la tournée se rend à ce pressoir et elle est partie (en cours ou livrée) ; une benne n'est reçue qu'une fois ;
 * - le poids ne dépasse pas la tolérance des paramètres (110 % de la capacité de la benne par défaut) : au-delà,
 *   la pesée est incohérente et refusée ;
 * - la catégorie de qualité est déduite de l'acidité ; le taux de remplissage = poids ÷ capacité de la benne ;
 * - le poids est réparti entre les vergers de la tournée au prorata de leurs quantités (décision 11 : en attendant les
 *   quantités saisies par le chef d'équipe, ce sont les quantités estimées) ;
 * - quand toutes les bennes de la tournée sont reçues, la tournée est « clôturée » et ses demandes « terminées » :
 *   chaque agriculteur reçoit le poids de son verger, l'acidité et la catégorie.
 * Choix retenu : une benne fait un seul voyage par tournée (le deuxième voyage n'est pas géré).
 */
@Service
@RequiredArgsConstructor
public class ReceptionService {

    /** Une tournée qui se rend au pressoir, et ses bennes (reçues ou non). */
    public record TourneeAttendue(String id, String numero, LocalDate date, StatutTournee statut, String libelleStatut,
            String nomChefEquipe, int quantiteEstimeeKg, int recuKg, boolean recevable, List<BenneAttendue> bennes) {
    }

    public record BenneAttendue(String id, String code, int capaciteKg, boolean recue) {
    }

    /** Une réception telle qu'affichée, et si elle a clôturé la tournée. */
    public record ReceptionReponse(String id, String numero, String tourneeId, String numeroTournee, String codeBenne,
            int capaciteBenneKg, int poidsNetKg, double aciditePourcent, CategorieQualite categorie, String libelleCategorie,
            int tauxRemplissage, Instant dateHeure, String nomAuteur, String commentaire, List<Reception.Part> parts,
            boolean tourneeCloturee) {

        static ReceptionReponse depuis(Reception reception, boolean tourneeCloturee) {
            return new ReceptionReponse(reception.getId(), reception.getNumero(), reception.getTourneeId(),
                    reception.getNumeroTournee(), reception.getCodeBenne(), reception.getCapaciteBenneKg(),
                    reception.getPoidsNetKg(), reception.getAciditePourcent(), reception.getCategorie(),
                    reception.getCategorie().getLibelle(), reception.getTauxRemplissage(), reception.getDateHeure(),
                    reception.getNomAuteur(), reception.getCommentaire(), reception.getParts(), tourneeCloturee);
        }
    }

    private static final Set<StatutTournee> RECEVABLES = Set.of(StatutTournee.EN_COURS, StatutTournee.LIVREE);

    private final ReceptionRepository receptionRepository;
    private final TourneeRepository tourneeRepository;
    private final DemandeRecolteRepository demandeRepository;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;
    private final Clock horloge;

    // --- Livraisons attendues ---

    /** Tournées du jour vers ce pressoir (sauf annulées), avec leurs bennes et ce qui en a été reçu. */
    public List<TourneeAttendue> attendues(Pressoir pressoir, LocalDate date) {
        List<Tournee> tournees = mongoTemplate.find(Query.query(Criteria.where("pressoirId").is(pressoir.getId())
                .and("date").is(date).and("statut").in(StatutTournee.ACTIVES)).with(Sort.by("numero")), Tournee.class);
        Map<String, List<Reception>> receptions = mongoTemplate.find(Query.query(Criteria.where("tourneeId")
                        .in(tournees.stream().map(Tournee::getId).toList())), Reception.class).stream()
                .collect(Collectors.groupingBy(Reception::getTourneeId));
        return tournees.stream().map(tournee -> {
            List<Reception> recues = receptions.getOrDefault(tournee.getId(), List.of());
            Set<String> bennesRecues = recues.stream().map(Reception::getBenneId).collect(Collectors.toSet());
            List<BenneAttendue> bennes = bennes(tournee).stream()
                    .map(benne -> new BenneAttendue(benne.ressourceId(), benne.code(), capacite(benne),
                            bennesRecues.contains(benne.ressourceId())))
                    .toList();
            return new TourneeAttendue(tournee.getId(), tournee.getNumero(), tournee.getDate(), tournee.getStatut(),
                    tournee.getStatut().getLibelle(), tournee.getNomChefEquipe(), tournee.quantiteEstimeeKg(),
                    recues.stream().mapToInt(Reception::getPoidsNetKg).sum(), RECEVABLES.contains(tournee.getStatut()), bennes);
        }).toList();
    }

    // --- Réception d'une benne ---

    public ReceptionReponse receptionner(ReceptionRequete requete, Pressoir pressoir, Utilisateur auteur) {
        Tournee tournee = tourneeRepository.findById(requete.tourneeId()).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "TOURNEE_INTROUVABLE", "Cette tournée n'existe pas."));
        if (!tournee.getPressoirId().equals(pressoir.getId())) {
            throw new ErreurMetierException(HttpStatus.FORBIDDEN, "TOURNEE_D_UN_AUTRE_PRESSOIR",
                    "La tournée " + tournee.getNumero() + " se rend à un autre pressoir (" + tournee.getNomPressoir() + ").");
        }
        if (!RECEVABLES.contains(tournee.getStatut())) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "TOURNEE_NON_RECEVABLE", tournee.getStatut() == StatutTournee.PLANIFIEE
                    ? "La tournée " + tournee.getNumero() + " n'a pas encore démarré : ses bennes ne peuvent pas être reçues."
                    : "La tournée " + tournee.getNumero() + " est " + tournee.getStatut().getLibelle().toLowerCase(Locale.FRENCH) + ".");
        }
        Tournee.Affectation benne = bennes(tournee).stream().filter(candidate -> candidate.ressourceId().equals(requete.benneId()))
                .findFirst().orElseThrow(() -> erreurChamp(HttpStatus.BAD_REQUEST, "benneId",
                        "Cette benne n'est pas affectée à la tournée " + tournee.getNumero() + "."));
        if (receptionRepository.existsByTourneeIdAndBenneId(tournee.getId(), benne.ressourceId())) {
            throw erreurChamp(HttpStatus.CONFLICT, "benneId", "La benne " + benne.code() + " de cette tournée a déjà été reçue.");
        }
        int capacite = capacite(benne);
        int tolerance = parametresService.actuels().regles().tolerancePesee();
        if (capacite > 0 && requete.poidsNetKg() * 100L > (long) capacite * tolerance) {
            throw erreurChamp(HttpStatus.BAD_REQUEST, "poidsNetKg", "Pesée incohérente : plus de " + tolerance
                    + " % de la capacité de la benne " + benne.code() + " (" + capacite + " kg). Refaites la pesée.");
        }

        double acidite = Math.round(requete.aciditePourcent() * 100) / 100.0;
        Reception reception = new Reception();
        reception.setNumero(compteurService.prochainCode("RECEPTION", "R-%04d"));
        reception.setPressoirId(pressoir.getId());
        reception.setTourneeId(tournee.getId());
        reception.setNumeroTournee(tournee.getNumero());
        reception.setBenneId(benne.ressourceId());
        reception.setCodeBenne(benne.code());
        reception.setCapaciteBenneKg(capacite);
        reception.setPoidsNetKg(requete.poidsNetKg());
        reception.setAciditePourcent(acidite);
        reception.setCategorie(CategorieQualite.pourAcidite(acidite));
        reception.setTauxRemplissage(capacite == 0 ? 0 : (int) Math.round(requete.poidsNetKg() * 100.0 / capacite));
        reception.setJour(LocalDate.now(horloge));
        reception.setDateHeure(horloge.instant());
        reception.setAuteurId(auteur.getId());
        reception.setNomAuteur(auteur.getNomComplet());
        reception.setCommentaire(requete.commentaire() == null || requete.commentaire().isBlank() ? null : requete.commentaire().trim());
        reception.setParts(repartir(tournee, requete.poidsNetKg()));
        reception.setVergerIds(reception.getParts().stream().map(Reception.Part::vergerId).distinct().toList());
        Reception enregistree = receptionRepository.save(reception);

        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE, NouvelleNotification.simple(CategorieNotification.COLLECTE,
                "Benne " + benne.code() + " reçue · " + tournee.getNumero(),
                pressoir.getNom() + " · " + requete.poidsNetKg() + " kg · acidité " + acidite + " % ("
                        + enregistree.getCategorie().getLibelle() + ")", "/collecte/tournees"));

        boolean cloturee = toutesLesBennesRecues(tournee);
        if (cloturee) {
            cloturer(tournee, auteur);
        }
        return ReceptionReponse.depuis(enregistree, cloturee);
    }

    // --- Historique ---

    public PageResultat<ReceptionReponse> historique(Pressoir pressoir, LocalDate debut, LocalDate fin, int page, int taille) {
        PageRequest pagination = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, 100),
                Sort.by(Sort.Direction.DESC, "dateHeure"));
        Query requete = new Query(periode(pressoir, debut, fin)).with(pagination);
        List<ReceptionReponse> elements = mongoTemplate.find(requete, Reception.class).stream()
                .map(reception -> ReceptionReponse.depuis(reception, false)).toList();
        var resultat = PageableExecutionUtils.getPage(elements, pagination,
                () -> mongoTemplate.count(Query.of(requete).limit(-1).skip(-1), Reception.class));
        return PageResultat.depuis(resultat, element -> element);
    }

    /** Toutes les réceptions de la période, les plus anciennes d'abord (export CSV). */
    public List<Reception> toutes(Pressoir pressoir, LocalDate debut, LocalDate fin) {
        return mongoTemplate.find(new Query(periode(pressoir, debut, fin)).with(Sort.by("dateHeure")), Reception.class);
    }

    private static Criteria periode(Pressoir pressoir, LocalDate debut, LocalDate fin) {
        return Criteria.where("pressoirId").is(pressoir.getId()).and("jour").gte(debut).lte(fin);
    }

    // --- Clôture ---

    private boolean toutesLesBennesRecues(Tournee tournee) {
        Set<String> recues = receptionRepository.findByTourneeIdOrderByDateHeureAsc(tournee.getId()).stream()
                .map(Reception::getBenneId).collect(Collectors.toSet());
        return bennes(tournee).stream().allMatch(benne -> recues.contains(benne.ressourceId()));
    }

    /** Tournée « clôturée », demandes « terminées » avec leur résultat ; agriculteurs, chef et collecte prévenus. */
    private void cloturer(Tournee tournee, Utilisateur auteur) {
        List<Reception> receptions = receptionRepository.findByTourneeIdOrderByDateHeureAsc(tournee.getId());
        int total = receptions.stream().mapToInt(Reception::getPoidsNetKg).sum();
        tournee.changerStatut(StatutTournee.CLOTUREE, horloge.instant(), auteur.getId(),
                receptions.size() + " benne(s) reçue(s) · " + total + " kg");
        tourneeRepository.save(tournee);

        // Poids et acidité pondérée de chaque demande, sur toutes les bennes de la tournée
        Map<String, int[]> poidsParDemande = new LinkedHashMap<>();
        Map<String, double[]> aciditeParDemande = new LinkedHashMap<>();
        for (Reception reception : receptions) {
            for (Reception.Part part : reception.getParts()) {
                poidsParDemande.computeIfAbsent(part.demandeId(), cle -> new int[1])[0] += part.poidsKg();
                aciditeParDemande.computeIfAbsent(part.demandeId(), cle -> new double[1])[0] += part.poidsKg() * reception.getAciditePourcent();
            }
        }
        for (Tournee.Etape etape : tournee.getEtapes()) {
            demandeRepository.findById(etape.demandeId())
                    .filter(demande -> demande.getStatut() == StatutDemande.PLANIFIEE || demande.getStatut() == StatutDemande.EN_COURS)
                    .ifPresent(demande -> terminer(demande, poidsParDemande.getOrDefault(demande.getId(), new int[1])[0],
                            aciditeParDemande.getOrDefault(demande.getId(), new double[1])[0], tournee, auteur));
        }
        NouvelleNotification cloture = NouvelleNotification.simple(CategorieNotification.COLLECTE,
                "Tournée " + tournee.getNumero() + " clôturée",
                tournee.getNomPressoir() + " · " + receptions.size() + " benne(s) · " + total + " kg reçus", "/collecte/tournees");
        notificationService.notifierPersonne(tournee.getChefEquipeId(), cloture);
        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE, cloture);
    }

    private void terminer(DemandeRecolte demande, int poids, double sommeAcidites, Tournee tournee, Utilisateur auteur) {
        double acidite = poids == 0 ? 0 : Math.round(sommeAcidites / poids * 100) / 100.0;
        CategorieQualite categorie = CategorieQualite.pourAcidite(acidite);
        demande.setPoidsRecuKg(poids);
        demande.setAciditeMoyenne(acidite);
        demande.setCategorieQualite(categorie);
        demande.changerStatut(StatutDemande.TERMINEE, horloge.instant(), auteur.getId(),
                poids + " kg reçus au pressoir " + tournee.getNomPressoir() + " · acidité " + acidite + " %");
        demandeRepository.save(demande);
        notificationService.notifierPersonne(demande.getAgriculteurId(), NouvelleNotification.simple(CategorieNotification.COLLECTE,
                        "Olives pesées · " + demande.getNomVerger(),
                        poids + " kg reçus au pressoir " + tournee.getNomPressoir() + " · acidité " + acidite + " % · qualité "
                                + categorie.getLibelle().toLowerCase(Locale.FRENCH) + ".",
                        "/agriculteur/demandes")
                .avecSms("OliveConnect : " + poids + " kg de votre verger " + demande.getNomVerger() + " reçus au pressoir "
                        + tournee.getNomPressoir() + " (acidité " + acidite + " %, " + categorie.getLibelle().toLowerCase(Locale.FRENCH) + ")."));
    }

    // --- Outils ---

    /**
     * Répartit le poids d'une benne entre les vergers de la tournée, au prorata de leurs quantités estimées.
     * Le reste de l'arrondi va au dernier verger, pour que la somme des parts soit exactement le poids pesé.
     */
    static List<Reception.Part> repartir(Tournee tournee, int poids) {
        int totalEstime = tournee.quantiteEstimeeKg();
        List<Reception.Part> parts = new ArrayList<>();
        int distribue = 0;
        List<Tournee.Etape> etapes = tournee.getEtapes();
        for (int rang = 0; rang < etapes.size(); rang++) {
            Tournee.Etape etape = etapes.get(rang);
            int part = rang == etapes.size() - 1 ? poids - distribue
                    : totalEstime == 0 ? poids / etapes.size() : (int) ((long) poids * etape.quantiteEstimeeKg() / totalEstime);
            distribue += part;
            parts.add(new Reception.Part(etape.demandeId(), etape.vergerId(), etape.nomVerger(), etape.agriculteurId(), part));
        }
        return parts;
    }

    private static List<Tournee.Affectation> bennes(Tournee tournee) {
        return tournee.getAffectations().stream().filter(affectation -> affectation.type() == TypeRessource.BENNE).toList();
    }

    private static int capacite(Tournee.Affectation benne) {
        return benne.capaciteKg() == null ? 0 : benne.capaciteKg();
    }

    private static ErreurMetierException erreurChamp(HttpStatus statut, String champ, String message) {
        return new ErreurMetierException(statut, statut == HttpStatus.CONFLICT ? "BENNE_DEJA_RECUE" : "DONNEES_INVALIDES",
                message, Map.of(champ, message));
    }
}
