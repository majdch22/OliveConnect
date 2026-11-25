package tn.oliveconnect.connexion;

import jakarta.validation.constraints.NotBlank;

/**
 * Données saisies sur la page de connexion.
 *
 * @param identifiant e-mail ou numéro de téléphone
 * @param motDePasse  mot de passe en clair (transmis une seule fois, jamais enregistré)
 */
public record ConnexionRequete(
        @NotBlank(message = "Saisissez votre e-mail ou votre téléphone.") String identifiant,
        @NotBlank(message = "Saisissez votre mot de passe.") String motDePasse) {
}
