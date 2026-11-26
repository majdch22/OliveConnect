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
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
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
 * - modification et annulation seulement tant que la collecte n'a pas démarré (états Soumise, Validée, Planifiée).
 */
@Service
@RequiredArgsConstructor
public class DemandeRecolteService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DemandeRecolteRepository demandeRepository;
    private final VergerService vergerService;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
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
        return DemandeReponse.depuis(demandeRepository.save(demande));
    }

    public DemandeReponse modifier(String id, DemandeRequete requete, String agriculteurId) {
        DemandeRecolte demande = verifierProprietaire(id, agriculteurId);
        verifierModifiable(demande);
        Verger verger = vergerActif(requete.vergerId(), agriculteurId);
        verifierDate(requete);
        verifierPasDeDoublon(requete, id);
        remplir(demande, requete, verger);
        // Si quelqu'un a modifié la demande entre-temps, la version ne correspond plus : refus (409)
        return DemandeReponse.depuis(demandeRepository.save(demande));
    }

    public DemandeReponse annuler(String id, String motif, String agriculteurId) {
        DemandeRecolte demande = verifierProprietaire(id, agriculteurId);
        verifierModifiable(demande);
        String motifNettoye = motif == null || motif.isBlank() ? null : motif.trim();
        demande.setMotifAnnulation(motifNettoye);
        demande.changerStatut(StatutDemande.ANNULEE, horloge.instant(), agriculteurId, motifNettoye);
        return DemandeReponse.depuis(demandeRepository.save(demande));
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
