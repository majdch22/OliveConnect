package tn.oliveconnect.connexion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Formulaire « Changer mon mot de passe » : l'ancien est redemandé pour prouver que c'est bien le titulaire. */
public record ChangementMotDePasseRequete(
        @NotBlank(message = "Saisissez votre mot de passe actuel.")
        String motDePasseActuel,

        @NotBlank(message = "Choisissez un nouveau mot de passe.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "8 caractères minimum, dont au moins une lettre et un chiffre.")
        String nouveauMotDePasse,

        @NotBlank(message = "Confirmez le nouveau mot de passe.")
        String confirmation) {
}
