package tn.oliveconnect.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages propres à OliveConnect, lus dans application.yml (préfixe "oliveconnect").
 * Les valeurs sensibles (secret, mot de passe) viennent des variables d'environnement en production.
 */
@ConfigurationProperties(prefix = "oliveconnect")
public record OliveConnectProperties(Securite securite, PremierAdministrateur premierAdministrateur) {

    /**
     * Réglages de la connexion.
     * Le nombre de tentatives et la durée de blocage passeront dans les paramètres modifiables par l'administrateur.
     */
    public record Securite(
            String secretJeton,
            Duration dureeJeton,
            int tentativesAvantBlocage,
            Duration dureeBlocage) {
    }

    /** Compte administrateur créé automatiquement au premier démarrage. */
    public record PremierAdministrateur(
            String nom,
            String prenom,
            String email,
            String telephone,
            String motDePasse) {
    }
}
