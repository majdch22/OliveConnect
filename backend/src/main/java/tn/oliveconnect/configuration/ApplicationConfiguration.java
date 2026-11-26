package tn.oliveconnect.configuration;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import tn.oliveconnect.OliveConnectApplication;

/**
 * Réglages généraux de l'application.
 * - Dates de création remplies automatiquement (@CreatedDate).
 * - Tâches planifiées activées (mesures de supervision, sauvegarde de la nuit).
 */
@Configuration
@EnableMongoAuditing
@EnableScheduling
public class ApplicationConfiguration {

    /**
     * Horloge unique de l'application, au fuseau de la Tunisie.
     * Les services la reçoivent au lieu d'appeler Instant.now(), ce qui permet aux tests de fixer l'heure.
     */
    @Bean
    public Clock horloge() {
        return Clock.system(ZoneId.of(OliveConnectApplication.FUSEAU_HORAIRE));
    }
}
