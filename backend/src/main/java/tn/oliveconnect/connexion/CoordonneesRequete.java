package tn.oliveconnect.connexion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import tn.oliveconnect.utilisateur.Delegation;

/**
 * « Mon profil » : téléphone, e-mail et délégation (agriculteur seulement).
 * Le mot de passe est redemandé : le téléphone et l'e-mail servent à se connecter et à recevoir les codes.
 */
public record CoordonneesRequete(
        @NotBlank(message = "Saisissez votre numéro de téléphone.") String telephone,
        @Email(message = "Cette adresse e-mail n'est pas valide.") String email,
        Delegation delegation,
        @NotBlank(message = "Saisissez votre mot de passe pour confirmer.") String motDePasse) {
}
