package tn.oliveconnect.demande;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.tournee.TourneeService;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerService;

/**
 * Demandes de récolte d'un agriculteur.
 *
 * Règles :
 * - le verger appartient à l'agriculteur et n'est pas archivé ;
 * - une récolte urgente peut être demandée pour aujourd'hui ; une récolte planifiée à partir de demain ;
 * - la date est comprise dans la campagne de récolte (paramètres de l'administrateur) ;
 * - une seule demande active par verger et par date ;
 * - modification et annulation seulement tant que la collecte n'a pas démarré (états Soumise, Validée, Planifiée) ;
 * - une demande planifiée annulée, ou dont la date ou le verger change, sort de sa tournée (la place est libérée) ;
 *   si seule la quantité change, la tournée reçoit la nouvelle quantité.
 * Les responsables de collecte sont prévenus de chaque déclaration (en priorité si elle est urgente),
 * modification et annulation.
 */
@Service
@RequiredArgsConstructor
public class DemandeRecolteService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Espace du responsable de collecte, ouvert en touchant la notification. */
    private static final String LIEN_COLLECTE = "/collecte";

    private final DemandeRecolteRepository demandeRepository;
    private final VergerService vergerService;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;
    private final TourneeService tourneeService;
    private final Clock horloge;

    public List<DemandeReponse> mesDemandes(String agriculteurId) {
        return demandeRepository.findByAgriculteurIdOrderByDateSouhaiteeDesc(agriculteurId).stream()
                .map(DemandeReponse::depuis).toList();
    }

    public DemandeReponse consulter(String id, String agriculteurId) {
        return DemandeReponse.depuis(verifierProprietaire(id, agriculteurId));
    }

    public DemandeReponse declarer(DemandeRequete requete, String agriculteurId) {
        Verger verger = vergerActif(requete.vergerId(), agriculteurId);
        verifierDate(requete);
        verifierPasDeDoublon(requete, "");

        DemandeRecolte demande = new DemandeRecolte();
        demande.setNumero(compteurService.prochainCode("DEMANDE", "D-%04d"));
        demande.setAgriculteurId(agriculteurId);
        remplir(demande, requete, verger);
        demande.setDateSoumission(horloge.instant());
        demande.changerStatut(StatutDemande.SOUMISE, horloge.instant(), agriculteurId, null);
        DemandeRecolte enregistree = demandeRepository.save(demande);
        boolean urgente = enregistree.getType() == TypeRecolte.URGENTE;
        NouvelleNotification contenu = NouvelleNotification.simple(CategorieNotification.COLLECTE,
                (urgente ? "Demande urgente " : "Nouvelle demande ") + enregistree.getNumero(), resume(enregistree), LIEN_COLLECTE);
        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE, urgente ? contenu.enPriorite() : contenu);
        return DemandeReponse.depuis(enregistree);
    }

    public DemandeReponse modifier(String id, DemandeRequete requete, String agriculteurId) {
        DemandeRecolte demande = verifierProprietaire(id, agriculteurId);
        verifierModifiable(demande);
        Verger verger = vergerActif(requete.vergerId(), agriculteurId);
        verifierDate(requete);
        verifierPasDeDoublon(requete, id);
        LocalDate ancienneDate = demande.getDateSouhaitee();
        String ancienVerger = demande.getVergerId();
        remplir(demande, requete, verger);
        boolean planifiee = demande.getStatut() == StatutDemande.PLANIFIEE;
        boolean deplacee = planifiee && (!ancienneDate.equals(demande.getDateSouhaitee()) || !ancienVerger.equals(demande.getVergerId()));
        if (deplacee) {
            tourneeService.retirerDemande(demande, "date ou verger modifié par l'agriculteur");
            demande.changerStatut(StatutDemande.VALIDEE, horloge.instant(), agriculteurId,
                    "Date ou verger modifié : collecte à replanifier");
        }
        // Si quelqu'un a modifié la demande entre-temps, la version ne correspond plus : refus (409)
        DemandeRecolte enregistree = demandeRepository.save(demande);
        if (planifiee && !deplacee) {
            tourneeService.mettreAJourQuantite(enregistree);
        }
        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE, NouvelleNotification.simple(CategorieNotification.COLLECTE,
                "Demande " + enregistree.getNumero() + " modifiée", resume(enregistree), LIEN_COLLECTE));
        return DemandeReponse.depuis(enregistree);
    }

    public DemandeReponse annuler(String id, String motif, String agriculteurId) {
        DemandeRecolte demande = verifierProprietaire(id, agriculteurId);
        verifierModifiable(demande);
        String motifNettoye = motif == null || motif.isBlank() ? null : motif.trim();
        if (demande.getStatut() == StatutDemande.PLANIFIEE) {
            tourneeService.retirerDemande(demande, "demande annulée par l'agriculteur");
        }
        demande.setMotifAnnulation(motifNettoye);
        demande.changerStatut(StatutDemande.ANNULEE, horloge.instant(), agriculteurId, motifNettoye);
        DemandeRecolte enregistree = demandeRepository.save(demande);
        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE, NouvelleNotification.simple(CategorieNotification.COLLECTE,
                "Demande " + enregistree.getNumero() + " annulée",
                resume(enregistree) + (motifNettoye == null ? "" : " · Motif : " + motifNettoye), LIEN_COLLECTE));
        return DemandeReponse.depuis(enregistree);
    }

    /**
     * Demande urgente créée par un responsable de collecte pour le verger d'un signalement (maturité critique, ravageur…).
     * Mêmes règles de date et de doublon que pour l'agriculteur ; la demande est directement « validée » puisque
     * c'est la coopérative qui la crée. L'agriculteur est prévenu.
     */
    public DemandeRecolte creerPourLaCooperative(Verger verger, LocalDate date, int quantiteKg, String commentaire,
            String auteurId) {
        if (verger.isArchive()) {
            throw erreurChamp(HttpStatus.BAD_REQUEST, "dateSouhaitee", "Ce verger est archivé.");
        }
        DemandeRequete requete = new DemandeRequete(verger.getId(), TypeRecolte.URGENTE, date, quantiteKg, commentaire);
        verifierDate(requete);
        verifierPasDeDoublon(requete, "");

        DemandeRecolte demande = new DemandeRecolte();
        demande.setNumero(compteurService.prochainCode("DEMANDE", "D-%04d"));
        demande.setAgriculteurId(verger.getAgriculteurId());
        demande.setCreeeParLaCooperative(true);
        remplir(demande, requete, verger);
        demande.setDateSoumission(horloge.instant());
        demande.changerStatut(StatutDemande.SOUMISE, horloge.instant(), auteurId, "Créée par la coopérative");
        demande.changerStatut(StatutDemande.VALIDEE, horloge.instant(), auteurId, null);
        DemandeRecolte enregistree = demandeRepository.save(demande);
        notificationService.notifierPersonne(verger.getAgriculteurId(), NouvelleNotification.simple(CategorieNotification.COLLECTE,
                        "Récolte urgente prévue · " + verger.getNom(),
                        "La coopérative a créé la demande " + enregistree.getNumero() + " pour le "
                                + date.format(FORMAT_DATE) + ". Elle sera planifiée en priorité.",
                        "/agriculteur/demandes")
                .avecSms("OliveConnect : une récolte urgente de votre verger " + verger.getNom() + " est prévue à partir du "
                        + date.format(FORMAT_DATE) + "."));
        return enregistree;
    }

    /** « Ali Ben Salah · Verger Nord · 02/12/2026 · 3500 kg » : le texte des notifications d'une demande. */
    private String resume(DemandeRecolte demande) {
        String agriculteur = utilisateurRepository.findById(demande.getAgriculteurId()).map(Utilisateur::getNomComplet).orElse("");
        return agriculteur + " · " + demande.getNomVerger() + " · " + demande.getDateSouhaitee().format(FORMAT_DATE)
                + " · " + demande.getQuantiteEstimeeKg() + " kg";
    }

    // --- Règles ---

    private DemandeRecolte verifierProprietaire(String id, String agriculteurId) {
        DemandeRecolte demande = demandeRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "DEMANDE_INTROUVABLE", "Cette demande n'existe pas."));
        if (!demande.getAgriculteurId().equals(agriculteurId)) {
            throw new ErreurMetierException(HttpStatus.FORBIDDEN, "DEMANDE_D_UN_AUTRE_AGRICULTEUR",
                    "Cette demande ne vous appartient pas.");
        }
        return demande;
    }

    private Verger vergerActif(String vergerId, String agriculteurId) {
        Verger verger = vergerService.verifierProprietaire(vergerId, agriculteurId);
        if (verger.isArchive()) {
            throw erreurChamp(HttpStatus.BAD_REQUEST, "vergerId", "Ce verger est archivé.");
        }
        return verger;
    }

    private void verifierDate(DemandeRequete requete) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        LocalDate datePlusTot = requete.type() == TypeRecolte.URGENTE ? aujourdhui : aujourdhui.plusDays(1);
        if (requete.dateSouhaitee().isBefore(datePlusTot)) {
            throw erreurChamp(HttpStatus.BAD_REQUEST, "dateSouhaitee", requete.type() == TypeRecolte.URGENTE
                    ? "Une récolte urgente ne peut pas être demandée pour une date passée."
                    : "Une récolte planifiée se demande au plus tôt pour demain.");
        }
        Parametres.Campagne campagne = parametresService.actuels().campagne();
        if (requete.dateSouhaitee().isBefore(campagne.debut()) || requete.dateSouhaitee().isAfter(campagne.fin())) {
            throw erreurChamp(HttpStatus.BAD_REQUEST, "dateSouhaitee", "La date doit être comprise dans la campagne de récolte, du "
                    + campagne.debut().format(FORMAT_DATE) + " au " + campagne.fin().format(FORMAT_DATE) + ".");
        }
    }

    private void verifierPasDeDoublon(DemandeRequete requete, String demandeModifiee) {
        if (demandeRepository.existsByVergerIdAndDateSouhaiteeAndStatutInAndIdNot(requete.vergerId(),
                requete.dateSouhaitee(), StatutDemande.ACTIFS, demandeModifiee)) {
            throw erreurChamp(HttpStatus.CONFLICT, "dateSouhaitee",
                    "Une demande est déjà en cours pour ce verger à cette date.");
        }
    }

    private static void verifierModifiable(DemandeRecolte demande) {
        if (!demande.estModifiable()) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "DEMANDE_NON_MODIFIABLE",
                    "Cette demande ne peut plus être modifiée : la collecte a démarré ou la demande est close.");
        }
    }

    private static void remplir(DemandeRecolte demande, DemandeRequete requete, Verger verger) {
        demande.setVergerId(verger.getId());
        demande.setNomVerger(verger.getNom());
        demande.setType(requete.type());
        demande.setDateSouhaitee(requete.dateSouhaitee());
        demande.setQuantiteEstimeeKg(requete.quantiteEstimeeKg());
        demande.setCommentaire(requete.commentaire() == null || requete.commentaire().isBlank() ? null : requete.commentaire().trim());
    }

    private static ErreurMetierException erreurChamp(HttpStatus statut, String champ, String message) {
        return new ErreurMetierException(statut, statut == HttpStatus.CONFLICT ? "DEMANDE_EN_DOUBLE" : "DONNEES_INVALIDES",
                message, Map.of(champ, message));
    }
}
