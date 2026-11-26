package tn.oliveconnect.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages propres à OliveConnect, lus dans application.yml (préfixe "oliveconnect").
 * Les valeurs sensibles (secret, mot de passe) viennent des variables d'environnement en production.
 */
@ConfigurationProperties(prefix = "oliveconnect")
public record OliveConnectProperties(
        Securite securite,
        PremierAdministrateur premierAdministrateur,
        Sauvegarde sauvegarde,
        Supervision supervision) {

    /**
     * Réglages de la connexion.
     * La durée de session, les tentatives et la durée de blocage ne sont que les valeurs du premier démarrage :
     * l'administrateur les modifie ensuite dans les paramètres.
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

    /**
     * Sauvegardes de la base avec l'outil mongodump.
     *
     * @param dossier         dossier où sont rangées les sauvegardes
     * @param outilMongodump  chemin de l'outil mongodump
     * @param conservation    nombre de sauvegardes gardées (les plus anciennes sont supprimées)
     * @param planification   moment de la sauvegarde automatique (expression cron)
     */
    public record Sauvegarde(String dossier, String outilMongodump, int conservation, String planification) {
    }

    /** @param adresseGrafana adresse du tableau Grafana (vide tant que Grafana n'est pas installé) */
    public record Supervision(String adresseGrafana) {
    }
}
