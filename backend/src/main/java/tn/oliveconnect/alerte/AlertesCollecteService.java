package tn.oliveconnect.alerte;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.Document;
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
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandeRecolteService;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;

/**
 * Traitement des signalements par le responsable de collecte : ouvert → pris en charge → résolu.
 * - Chaque action est gardée dans le signalement (date, auteur, commentaire) et l'agriculteur en est prévenu.
 * - Résoudre demande un commentaire : c'est la réponse que l'agriculteur lit sous son signalement.
 * - « Créer une demande urgente » (point tranché pendant le codage) : la coopérative crée pour le verger du
 *   signalement une demande urgente directement validée ; le signalement passe « pris en charge ».
 */
@Service
@RequiredArgsConstructor
public class AlertesCollecteService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Un signalement vu par la coopérative, avec son agriculteur, son verger et ses traitements. */
    public record AlerteCollecteReponse(
            String id, String numero, TypeAlerte type, String libelleType, StatutAlerte statut, String libelleStatut,
            Instant dateCreation, String description, boolean avecPhoto, double latitude, double longitude,
            int distanceAuVergerMetres, boolean loinDuVerger,
            String vergerId, String nomVerger, Integer niveauMaturite,
            String agriculteurId, String nomAgriculteur, String telephoneAgriculteur,
            String demandeUrgenteId, String numeroDemandeUrgente, Instant datePriseEnCharge, Instant dateResolution,
            List<EtapeTraitement> traitements) {

        public record EtapeTraitement(String libelleStatut, Instant date, String nomAuteur, String commentaire) {
        }
    }

    private final AlerteRepository alerteRepository;
    private final VergerRepository vergerRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeRecolteService demandeService;
    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;
    private final Clock horloge;

    // --- Lecture ---

    /** File des signalements, les plus récents d'abord (index statut + date). */
    public PageResultat<AlerteCollecteReponse> lister(StatutAlerte statut, TypeAlerte type, int page, int taille) {
        Criteria criteres = new Criteria();
        if (statut != null) {
            criteres.and("statut").is(statut);
        }
        if (type != null) {
            criteres.and("type").is(type);
        }
        PageRequest pagination = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, 50),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        Query requete = new Query(criteres).with(pagination);
        List<Alerte> alertes = mongoTemplate.find(requete, Alerte.class);
        var resultat = PageableExecutionUtils.getPage(enrichir(alertes), pagination,
                () -> mongoTemplate.count(Query.of(requete).limit(-1).skip(-1), Alerte.class));
        return PageResultat.depuis(resultat, Function.identity());
    }

    /** Nombre de signalements par état (onglets). Agrégation : { $group: { _id: "$statut", nombre: { $sum: 1 } } }. */
    public Map<String, Long> compteurs() {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (StatutAlerte statut : StatutAlerte.values()) {
            compteurs.put(statut.name(), 0L);
        }
        for (Document ligne : mongoTemplate.aggregate(newAggregation(group("statut").count().as("nombre")), "alertes",
                Document.class)) {
            compteurs.put(ligne.getString("_id"), ((Number) ligne.get("nombre")).longValue());
        }
        return compteurs;
    }

    public AlerteCollecteReponse consulter(String id) {
        return enrichir(List.of(alerte(id))).getFirst();
    }

    // --- Actions ---

    public AlerteCollecteReponse prendreEnCharge(String id, String commentaire, String auteurId) {
        Alerte alerte = alerte(id);
        if (alerte.getStatut() != StatutAlerte.OUVERTE) {
            throw dejaTraitee(alerte);
        }
        String commentaireNettoye = nettoyer(commentaire);
        alerte.setDatePriseEnCharge(horloge.instant());
        alerte.traiter(StatutAlerte.PRISE_EN_CHARGE, horloge.instant(), auteurId, nomDe(auteurId), commentaireNettoye);
        Alerte enregistree = alerteRepository.save(alerte);
        prevenirAgriculteur(enregistree, "pris en charge", commentaireNettoye);
        return consulter(enregistree.getId());
    }

    public AlerteCollecteReponse resoudre(String id, String commentaire, String auteurId) {
        Alerte alerte = alerte(id);
        if (alerte.getStatut() == StatutAlerte.RESOLUE) {
            throw dejaTraitee(alerte);
        }
        Instant maintenant = horloge.instant();
        if (alerte.getDatePriseEnCharge() == null) {
            alerte.setDatePriseEnCharge(maintenant);
        }
        alerte.setDateResolution(maintenant);
        alerte.traiter(StatutAlerte.RESOLUE, maintenant, auteurId, nomDe(auteurId), commentaire.trim());
        Alerte enregistree = alerteRepository.save(alerte);
        prevenirAgriculteur(enregistree, "résolu", commentaire.trim());
        return consulter(enregistree.getId());
    }

    public AlerteCollecteReponse creerDemandeUrgente(String id, LocalDate date, int quantiteKg, String commentaire,
            String auteurId) {
        Alerte alerte = alerte(id);
        if (alerte.getStatut() == StatutAlerte.RESOLUE) {
            throw dejaTraitee(alerte);
        }
        if (alerte.getDemandeUrgenteId() != null) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "DEMANDE_DEJA_CREEE",
                    "La demande " + alerte.getNumeroDemandeUrgente() + " a déjà été créée pour ce signalement.");
        }
        Verger verger = vergerRepository.findById(alerte.getVergerId()).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "VERGER_INTROUVABLE", "Le verger de ce signalement n'existe plus."));
        String texte = nettoyer(commentaire);
        DemandeRecolte demande = demandeService.creerPourLaCooperative(verger, date, quantiteKg,
                "Suite au signalement " + alerte.getNumero() + (texte == null ? "" : " · " + texte), auteurId);

        Instant maintenant = horloge.instant();
        alerte.setDemandeUrgenteId(demande.getId());
        alerte.setNumeroDemandeUrgente(demande.getNumero());
        if (alerte.getDatePriseEnCharge() == null) {
            alerte.setDatePriseEnCharge(maintenant);
        }
        alerte.traiter(StatutAlerte.PRISE_EN_CHARGE, maintenant, auteurId, nomDe(auteurId),
                "Récolte urgente prévue à partir du " + date.format(FORMAT_DATE) + " (demande " + demande.getNumero() + ")");
        alerteRepository.save(alerte);
        return consulter(id);
    }

    // --- Outils ---

    private void prevenirAgriculteur(Alerte alerte, String etat, String commentaire) {
        notificationService.notifierPersonne(alerte.getAgriculteurId(), NouvelleNotification.simple(CategorieNotification.ALERTE,
                "Signalement " + alerte.getNumero() + " " + etat,
                alerte.getType().getLibelle() + " · " + alerte.getNomVerger() + (commentaire == null ? "" : " · " + commentaire),
                "/agriculteur/signalements"));
    }

    private List<AlerteCollecteReponse> enrichir(List<Alerte> alertes) {
        Map<String, Verger> vergers = vergerRepository.findAllById(
                        alertes.stream().map(Alerte::getVergerId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Verger::getId, Function.identity()));
        Set<String> personnes = new HashSet<>();
        alertes.forEach(alerte -> personnes.add(alerte.getAgriculteurId()));
        Map<String, Utilisateur> comptes = utilisateurRepository.findAllById(personnes).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Function.identity()));
        return alertes.stream().map(alerte -> {
            Verger verger = vergers.get(alerte.getVergerId());
            Utilisateur agriculteur = comptes.get(alerte.getAgriculteurId());
            return new AlerteCollecteReponse(alerte.getId(), alerte.getNumero(), alerte.getType(), alerte.getType().getLibelle(),
                    alerte.getStatut(), alerte.getStatut().getLibelle(), alerte.getDateCreation(), alerte.getDescription(),
                    alerte.getPhoto() != null, alerte.getPosition().getY(), alerte.getPosition().getX(),
                    alerte.getDistanceAuVergerMetres(), alerte.isLoinDuVerger(), alerte.getVergerId(), alerte.getNomVerger(),
                    verger == null ? null : verger.getNiveauMaturite(), alerte.getAgriculteurId(),
                    agriculteur == null ? "" : agriculteur.getNomComplet(), agriculteur == null ? null : agriculteur.getTelephone(),
                    alerte.getDemandeUrgenteId(), alerte.getNumeroDemandeUrgente(), alerte.getDatePriseEnCharge(),
                    alerte.getDateResolution(),
                    alerte.getTraitements().stream().map(traitement -> new AlerteCollecteReponse.EtapeTraitement(
                            traitement.statut().getLibelle(), traitement.date(), traitement.nomAuteur(), traitement.commentaire()))
                            .toList());
        }).toList();
    }

    private String nomDe(String compteId) {
        return utilisateurRepository.findById(compteId).map(Utilisateur::getNomComplet).orElse(null);
    }

    private Alerte alerte(String id) {
        return alerteRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "SIGNALEMENT_INTROUVABLE", "Ce signalement n'existe pas."));
    }

    private static ErreurMetierException dejaTraitee(Alerte alerte) {
        return new ErreurMetierException(HttpStatus.CONFLICT, "SIGNALEMENT_DEJA_TRAITE",
                "Le signalement " + alerte.getNumero() + " est déjà " + alerte.getStatut().getLibelle().toLowerCase() + ".");
    }

    private static String nettoyer(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }
}
