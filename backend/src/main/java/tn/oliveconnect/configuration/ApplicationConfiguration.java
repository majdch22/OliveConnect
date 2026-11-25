package tn.oliveconnect.configuration;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import tn.oliveconnect.OliveConnectApplication;

/** Réglages généraux de l'application. */
@Configuration
@EnableMongoAuditing
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
