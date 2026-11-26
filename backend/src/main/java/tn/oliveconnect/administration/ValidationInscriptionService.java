package tn.oliveconnect.administration;

import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.utilisateur.InfosAgriculteur;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Traitement des inscriptions d'agriculteurs par l'administrateur.
 * - Accepter : le compte devient actif et reçoit son numéro d'adhérent (ADH-0001, ADH-0002…).
 * - Refuser : le compte reste fermé, avec un motif obligatoire.
 * Un refus peut être annulé plus tard en acceptant l'inscription.
 */
@Service
@RequiredArgsConstructor
public class ValidationInscriptionService {

    private final UtilisateurRepository utilisateurRepository;
    private final GestionComptesService gestionComptesService;
    private final CompteurService compteurService;
    private final Clock horloge;

    public CompteAdministration accepter(String id, String administrateurId) {
        Utilisateur agriculteur = inscription(id);
        StatutCompte statut = agriculteur.getStatut();
        if (statut != StatutCompte.EN_ATTENTE_VALIDATION && statut != StatutCompte.REFUSE) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "INSCRIPTION_DEJA_ACCEPTEE",
                    "Cette inscription a déjà été acceptée.");
        }
        InfosAgriculteur infos = agriculteur.getAgriculteur();
        if (infos.getNumeroAdherent() == null) {
            infos.setNumeroAdherent(compteurService.prochainNumeroAdherent());
        }
        infos.setMotifRefus(null);
        noterTraitement(infos, administrateurId);
        agriculteur.setStatut(StatutCompte.ACTIF);
        utilisateurRepository.save(agriculteur);
        return CompteAdministration.depuis(agriculteur);
    }

    public CompteAdministration refuser(String id, String motif, String administrateurId) {
        Utilisateur agriculteur = inscription(id);
        if (agriculteur.getStatut() != StatutCompte.EN_ATTENTE_VALIDATION) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "INSCRIPTION_DEJA_TRAITEE",
                    "Seule une inscription en attente peut être refusée.");
        }
        InfosAgriculteur infos = agriculteur.getAgriculteur();
        infos.setMotifRefus(motif.trim());
        noterTraitement(infos, administrateurId);
        agriculteur.setStatut(StatutCompte.REFUSE);
        utilisateurRepository.save(agriculteur);
        return CompteAdministration.depuis(agriculteur);
    }

    /** Le compte doit exister et être celui d'un agriculteur. */
    private Utilisateur inscription(String id) {
        Utilisateur compte = gestionComptesService.trouver(id);
        if (compte.getRole() != Role.AGRICULTEUR || compte.getAgriculteur() == null) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "PAS_UNE_INSCRIPTION",
                    "Ce compte n'est pas une inscription d'agriculteur.");
        }
        return compte;
    }

    private void noterTraitement(InfosAgriculteur infos, String administrateurId) {
        infos.setDateTraitementInscription(horloge.instant());
        infos.setInscriptionTraiteePar(administrateurId);
    }
}
