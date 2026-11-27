package tn.oliveconnect.pressoir;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.referentiel.PressoirRepository;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Pressoir sur lequel agit la personne connectée (décisions 4 et R6) :
 * - un responsable du pressoir n'agit que sur le pressoir auquel son compte est rattaché ;
 * - l'administrateur, qui peut tout faire (R3), indique le pressoir dans la requête.
 */
@Component
@RequiredArgsConstructor
public class PressoirDuCompte {

    private final UtilisateurRepository utilisateurRepository;
    private final PressoirRepository pressoirRepository;

    /** La personne et son pressoir. */
    public record Contexte(Utilisateur compte, Pressoir pressoir) {
    }

    public Contexte resoudre(String compteId, String pressoirDemande) {
        Utilisateur compte = utilisateurRepository.findById(compteId).orElseThrow();
        String pressoirId = compte.getRole() == Role.RESPONSABLE_PRESSOIR ? compte.getPressoirId() : pressoirDemande;
        if (pressoirId == null) {
            throw compte.getRole() == Role.RESPONSABLE_PRESSOIR
                    ? new ErreurMetierException(HttpStatus.CONFLICT, "AUCUN_PRESSOIR",
                            "Votre compte n'est rattaché à aucun pressoir : demandez à l'administrateur de le faire.")
                    : new ErreurMetierException(HttpStatus.BAD_REQUEST, "PRESSOIR_A_PRECISER", "Indiquez le pressoir (pressoirId).");
        }
        Pressoir pressoir = pressoirRepository.findById(pressoirId).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "PRESSOIR_INTROUVABLE", "Ce pressoir n'existe pas."));
        return new Contexte(compte, pressoir);
    }
}
