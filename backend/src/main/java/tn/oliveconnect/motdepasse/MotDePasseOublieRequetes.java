package tn.oliveconnect.motdepasse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Les trois formulaires de « Mot de passe oublié ». */
public final class MotDePasseOublieRequetes {

    private MotDePasseOublieRequetes() {
    }

    /** Étape 1 : le téléphone ou l'e-mail du compte. */
    public record DemandeCode(
            @NotBlank(message = "Saisissez votre téléphone ou votre e-mail.") String identifiant) {
    }

    /** Étape 2 : le code reçu par SMS. */
    public record VerificationCode(
            @NotBlank(message = "Saisissez votre téléphone ou votre e-mail.") String identifiant,
            @NotBlank(message = "Saisissez le code reçu.")
            @Pattern(regexp = "^\\d{6}$", message = "Le code contient 6 chiffres.") String code) {
    }

    /** Étape 3 : le nouveau mot de passe (le code est renvoyé et vérifié une dernière fois). */
    public record NouveauMotDePasse(
            @NotBlank(message = "Saisissez votre téléphone ou votre e-mail.") String identifiant,
            @NotBlank(message = "Saisissez le code reçu.")
            @Pattern(regexp = "^\\d{6}$", message = "Le code contient 6 chiffres.") String code,
            @NotBlank(message = "Choisissez un nouveau mot de passe.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                    message = "8 caractères minimum, dont au moins une lettre et un chiffre.") String nouveauMotDePasse,
            @NotBlank(message = "Confirmez le nouveau mot de passe.") String confirmation) {
    }

    /**
     * Réponse de l'étape 1, toujours la même, que le compte existe ou non : personne ne peut s'en servir
     * pour savoir si un numéro est inscrit.
     */
    public record CodeDemande(String message, int delaiRenvoiSecondes, int dureeValiditeMinutes) {
    }
}
