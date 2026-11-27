package tn.oliveconnect.demande;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.lookup;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.alerte.StatutAlerte;
import tn.oliveconnect.common.Distances;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;

/**
 * Examen des demandes de récolte par le responsable de collecte.
 *
 * - File de traitement filtrée par état et par type, triée **urgentes d'abord, puis par maturité du verger**
 *   (la plus mûre en premier), puis par date souhaitée. Le tri par maturité demande de lire le verger : il est fait par
 *   MongoDB dans une agrégation ($lookup vers « vergers »), et seule la page demandée est renvoyée.
 * - Valider : une demande « soumise » devient « validée » (elle pourra être planifiée).
 * - Rejeter : une demande « soumise » ou « validée » devient « rejetée », avec un motif obligatoire envoyé à l'agriculteur.
 * L'agriculteur est prévenu de chaque décision (notification, et SMS selon les paramètres et son choix).
 */
@Service
@RequiredArgsConstructor
public class DemandesCollecteService {

    /** Une demande vue par la coopérative : la demande, son verger et son agriculteur. */
    public record DemandeCollecteReponse(
            String id, String numero, TypeRecolte type, String libelleType, StatutDemande statut, String libelleStatut,
            LocalDate dateSouhaitee, LocalDate datePlanifiee, String tourneeId, String numeroTournee, int quantiteEstimeeKg,
            String commentaire, String motifRejet, String motifAnnulation, Instant dateSoumission, boolean creeeParLaCooperative,
            String vergerId, String nomVerger, String codeVerger, String libelleDelegation, String nomVariete,
            double superficieHa, int nombreArbres, Integer niveauMaturite, String libelleStatutRecolte,
            double latitude, double longitude,
            String agriculteurId, String nomAgriculteur, String numeroAdherent, String telephoneAgriculteur,
            List<Etape> historique) {

        /** Un changement d'état, avec le nom de la personne qui l'a fait. */
        public record Etape(String libelleNouveau, Instant date, String nomAuteur, String commentaire) {
        }
    }

    /** Fiche complète : la demande et des repères pour décider (pressoir le plus proche, passé du verger). */
    public record FicheDemande(DemandeCollecteReponse demande, String pressoirLePlusProche, Integer distancePressoirKm,
            long collectesTermineesDuVerger, long signalementsNonResolusDuVerger) {
    }

    private final DemandeRecolteRepository demandeRepository;
    private final VergerRepository vergerRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;
    private final Clock horloge;

    // --- Lecture ---

    public PageResultat<DemandeCollecteReponse> lister(StatutDemande statut, TypeRecolte type, String texte, int page, int taille) {
        Criteria criteres = criteres(statut, type, texte);
        int numeroPage = Math.max(page, 0);
        int taillePage = Math.clamp(taille, 1, 50);
        long total = mongoTemplate.count(Query.query(criteres), DemandeRecolte.class);

        // L'identifiant du verger est rangé en texte dans la demande : on le convertit en ObjectId pour le $lookup
        AggregationOperation identifiantVerger = contexte -> new Document("$addFields",
                new Document("vergerObjet", new Document("$toObjectId", "$vergerId")));
        AggregationOperation maturiteVerger = contexte -> new Document("$addFields", new Document("maturiteVerger",
                new Document("$ifNull", List.of(new Document("$arrayElemAt", List.of("$verger.niveauMaturite", 0)), -1))));
        AggregationOperation tri = contexte -> new Document("$sort",
                new Document("type", -1).append("maturiteVerger", -1).append("dateSouhaitee", 1).append("dateSoumission", 1));
        Aggregation agregation = newAggregation(
                match(criteres),
                identifiantVerger,
                lookup("vergers", "vergerObjet", "_id", "verger"),
                maturiteVerger,
                tri,
                skip((long) numeroPage * taillePage),
                limit(taillePage));
        List<DemandeRecolte> demandes = mongoTemplate.aggregate(agregation, "demandes_recolte", DemandeRecolte.class)
                .getMappedResults();

        int totalPages = (int) Math.ceil((double) total / taillePage);
        return new PageResultat<>(enrichir(demandes), numeroPage, taillePage, total, totalPages);
    }

    /**
     * Nombre de demandes par état (onglets) et urgentes en attente d'une décision.
     * Agrégation MongoDB : { $group: { _id: "$statut", nombre: { $sum: 1 } } }.
     */
    public Map<String, Long> compteurs() {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (StatutDemande statut : StatutDemande.values()) {
            compteurs.put(statut.name(), 0L);
        }
        Aggregation agregation = newAggregation(group("statut").count().as("nombre"));
        for (Document ligne : mongoTemplate.aggregate(agregation, "demandes_recolte", Document.class)) {
            compteurs.put(ligne.getString("_id"), ((Number) ligne.get("nombre")).longValue());
        }
        compteurs.put("URGENTES_A_TRAITER", mongoTemplate.count(Query.query(where("type").is(TypeRecolte.URGENTE.name())
                .and("statut").in(StatutDemande.SOUMISE.name(), StatutDemande.VALIDEE.name())), DemandeRecolte.class));
        return compteurs;
    }

    public FicheDemande fiche(String id) {
        DemandeRecolte demande = demande(id);
        DemandeCollecteReponse reponse = enrichir(List.of(demande)).getFirst();
        Verger verger = vergerRepository.findById(demande.getVergerId()).orElse(null);
        String pressoirProche = null;
        Integer distanceKm = null;
        if (verger != null) {
            // $nearSphere sur l'index 2dsphere des pressoirs : le premier résultat est le plus proche
            Pressoir proche = mongoTemplate.findOne(Query.query(where("localisation").nearSphere(verger.getLocalisation())
                    .and("archive").is(false)), Pressoir.class);
            if (proche != null) {
                pressoirProche = proche.getNom();
                distanceKm = (int) Math.round(Distances.metres(verger.getLocalisation().getY(), verger.getLocalisation().getX(),
                        proche.getLocalisation().getY(), proche.getLocalisation().getX()) / 1000);
            }
        }
        long terminees = mongoTemplate.count(Query.query(where("vergerId").is(demande.getVergerId())
                .and("statut").is(StatutDemande.TERMINEE.name())), DemandeRecolte.class);
        long signalements = mongoTemplate.count(Query.query(where("vergerId").is(demande.getVergerId())
                .and("statut").in(StatutAlerte.NON_RESOLUS.stream().map(Enum::name).toList())), Alerte.class);
        return new FicheDemande(reponse, pressoirProche, distanceKm, terminees, signalements);
    }

    // --- Décisions ---

    public DemandeCollecteReponse valider(String id, String commentaire, String auteurId) {
        DemandeRecolte demande = demande(id);
        if (demande.getStatut() != StatutDemande.SOUMISE) {
            throw dejaTraitee(demande);
        }
        demande.changerStatut(StatutDemande.VALIDEE, horloge.instant(), auteurId, nettoyer(commentaire));
        DemandeRecolte enregistree = demandeRepository.save(demande);
        notificationService.notifierPersonne(enregistree.getAgriculteurId(), NouvelleNotification.simple(
                        CategorieNotification.COLLECTE, "Demande " + enregistree.getNumero() + " validée",
                        enregistree.getNomVerger() + " : la coopérative organise la collecte. La date vous sera confirmée.",
                        "/agriculteur/demandes")
                .avecSms("OliveConnect : votre demande " + enregistree.getNumero() + " (" + enregistree.getNomVerger()
                        + ") est validée. La date de collecte vous sera confirmée."));
        return enrichir(List.of(enregistree)).getFirst();
    }

    public DemandeCollecteReponse rejeter(String id, String motif, String auteurId) {
        DemandeRecolte demande = demande(id);
        if (demande.getStatut() != StatutDemande.SOUMISE && demande.getStatut() != StatutDemande.VALIDEE) {
            throw dejaTraitee(demande);
        }
        String motifNettoye = motif.trim();
        demande.setMotifRejet(motifNettoye);
        demande.changerStatut(StatutDemande.REJETEE, horloge.instant(), auteurId, motifNettoye);
        DemandeRecolte enregistree = demandeRepository.save(demande);
        notificationService.notifierPersonne(enregistree.getAgriculteurId(), NouvelleNotification.simple(
                        CategorieNotification.COLLECTE, "Demande " + enregistree.getNumero() + " non retenue",
                        enregistree.getNomVerger() + " · Motif : " + motifNettoye, "/agriculteur/demandes")
                .avecSms("OliveConnect : votre demande " + enregistree.getNumero() + " (" + enregistree.getNomVerger()
                        + ") n'est pas retenue. Motif : " + motifNettoye));
        return enrichir(List.of(enregistree)).getFirst();
    }

    // --- Outils ---

    private Criteria criteres(StatutDemande statut, TypeRecolte type, String texte) {
        Criteria criteres = new Criteria();
        if (statut != null) {
            criteres.and("statut").is(statut.name());
        }
        if (type != null) {
            criteres.and("type").is(type.name());
        }
        if (texte != null && !texte.isBlank()) {
            // Texte cherché tel quel (caractères spéciaux échappés), sans tenir compte des majuscules
            Pattern recherche = Pattern.compile(Pattern.quote(texte.trim()), Pattern.CASE_INSENSITIVE);
            criteres.orOperator(where("numero").regex(recherche), where("nomVerger").regex(recherche));
        }
        return criteres;
    }

    /** Ajoute à chaque demande son verger et son agriculteur (deux requêtes pour toute la page, pas une par ligne). */
    private List<DemandeCollecteReponse> enrichir(List<DemandeRecolte> demandes) {
        Map<String, Verger> vergers = vergerRepository.findAllById(
                        demandes.stream().map(DemandeRecolte::getVergerId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Verger::getId, Function.identity()));
        Set<String> personnes = new HashSet<>();
        demandes.forEach(demande -> {
            personnes.add(demande.getAgriculteurId());
            demande.getHistorique().forEach(changement -> personnes.add(changement.auteurId()));
        });
        personnes.remove(null);
        Map<String, Utilisateur> comptes = utilisateurRepository.findAllById(personnes).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Function.identity()));
        return demandes.stream().map(demande -> reponse(demande, vergers.get(demande.getVergerId()), comptes)).toList();
    }

    private static DemandeCollecteReponse reponse(DemandeRecolte demande, Verger verger, Map<String, Utilisateur> comptes) {
        Utilisateur agriculteur = comptes.get(demande.getAgriculteurId());
        List<DemandeCollecteReponse.Etape> historique = demande.getHistorique().stream()
                .map(changement -> {
                    Utilisateur auteur = comptes.get(changement.auteurId());
                    return new DemandeCollecteReponse.Etape(changement.nouveau().getLibelle(), changement.date(),
                            auteur == null ? null : auteur.getNomComplet(), changement.commentaire());
                })
                .toList();
        return new DemandeCollecteReponse(demande.getId(), demande.getNumero(), demande.getType(), demande.getType().getLibelle(),
                demande.getStatut(), demande.getStatut().getLibelle(), demande.getDateSouhaitee(), demande.getDatePlanifiee(),
                demande.getTourneeId(), demande.getNumeroTournee(), demande.getQuantiteEstimeeKg(), demande.getCommentaire(),
                demande.getMotifRejet(), demande.getMotifAnnulation(), demande.getDateSoumission(), demande.isCreeeParLaCooperative(),
                demande.getVergerId(), demande.getNomVerger(), verger == null ? null : verger.getCode(),
                verger == null || verger.getDelegation() == null ? null : verger.getDelegation().getLibelle(),
                verger == null ? null : verger.getNomVariete(), verger == null ? 0 : verger.getSuperficieHa(),
                verger == null ? 0 : verger.getNombreArbres(), verger == null ? null : verger.getNiveauMaturite(),
                verger == null ? null : verger.getStatutRecolte().getLibelle(),
                verger == null ? 0 : verger.getLocalisation().getY(), verger == null ? 0 : verger.getLocalisation().getX(),
                demande.getAgriculteurId(), agriculteur == null ? "" : agriculteur.getNomComplet(),
                agriculteur == null || agriculteur.getAgriculteur() == null ? null : agriculteur.getAgriculteur().getNumeroAdherent(),
                agriculteur == null ? null : agriculteur.getTelephone(), historique);
    }

    private DemandeRecolte demande(String id) {
        return demandeRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "DEMANDE_INTROUVABLE", "Cette demande n'existe pas."));
    }

    private static ErreurMetierException dejaTraitee(DemandeRecolte demande) {
        return new ErreurMetierException(HttpStatus.CONFLICT, "DEMANDE_DEJA_TRAITEE", "La demande " + demande.getNumero()
                + " a déjà été traitée (état : " + demande.getStatut().getLibelle().toLowerCase(Locale.FRENCH) + ").");
    }

    private static String nettoyer(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }
}
