package tn.oliveconnect.utilisateur;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;

/**
 * Vérifie que le téléphone, l'e-mail et la carte d'identité ne sont pas déjà pris par un autre compte.
 * Utilisée à l'inscription, à la création d'un compte par l'administrateur et à la modification d'un compte.
 */
@Component
@RequiredArgsConstructor
public class VerificationIdentifiants {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * @param telephone       téléphone déjà normalisé
     * @param email           e-mail déjà normalisé, ou null
     * @param cin             carte d'identité, ou null pour le personnel
     * @param compteModifie   identifiant du compte en cours de modification (ignoré dans la recherche), ou null
     */
    public void verifierDisponibles(String telephone, String email, String cin, String compteModifie) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        utilisateurRepository.findByTelephone(telephone)
                .filter(autre -> !Objects.equals(autre.getId(), compteModifie))
                .ifPresent(autre -> erreurs.put("telephone", "Ce numéro est déjà utilisé par un autre compte."));
        if (email != null) {
            utilisateurRepository.findByEmail(email)
                    .filter(autre -> !Objects.equals(autre.getId(), compteModifie))
                    .ifPresent(autre -> erreurs.put("email", "Cette adresse e-mail est déjà utilisée par un autre compte."));
        }
        if (cin != null && utilisateurRepository.existsByAgriculteurCin(cin)) {
            erreurs.put("cin", "Cette carte d'identité est déjà enregistrée.");
        }
        if (!erreurs.isEmpty()) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "DONNEES_DEJA_UTILISEES",
                    "Un compte existe déjà avec certaines de ces informations.", erreurs);
        }
    }

    /** Un numéro tunisien normalisé : +216 suivi de 8 chiffres. */
    public static boolean estTelephoneTunisien(String telephone) {
        return telephone != null && telephone.matches("\\+216\\d{8}");
    }
}
