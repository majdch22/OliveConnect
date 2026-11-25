package tn.oliveconnect;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OliveConnectApplication {

    /** Fuseau horaire unique de l'application (décision 22). */
    public static final String FUSEAU_HORAIRE = "Africa/Tunis";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(FUSEAU_HORAIRE));
        SpringApplication.run(OliveConnectApplication.class, args);
    }
}
