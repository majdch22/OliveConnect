package tn.oliveconnect.administration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.oliveconnect.utilisateur.Role;

/**
 * Formulaire de l'administrateur pour créer ou modifier un compte du personnel.
 * Le mot de passe n'en fait pas partie : il est généré par la plateforme (mot de passe provisoire).
 */
public record CompteRequete(
        @NotBlank(message = "Saisissez le nom.")
        @Size(max = 60, message = "60 caractères au maximum.")
        String nom,

        @NotBlank(message = "Saisissez le prénom.")
        @Size(max = 60, message = "60 caractères au maximum.")
        String prenom,

        /** Facultatif. */
        @Email(message = "Cette adresse e-mail n'est pas valide.")
        String email,

        @NotBlank(message = "Saisissez le numéro de téléphone.")
        String telephone,

        @NotNull(message = "Choisissez le rôle.")
        Role role,

        /** Obligatoire pour un responsable du pressoir, ignoré pour les autres rôles. */
        String pressoirId) {
}
