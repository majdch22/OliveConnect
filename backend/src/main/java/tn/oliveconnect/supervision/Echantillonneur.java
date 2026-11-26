package tn.oliveconnect.supervision;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

/**
 * Toutes les 10 secondes, relève les mesures du serveur et en garde une heure en mémoire pour les graphiques.
 * Les mesures viennent de Micrometer (fourni par Spring Boot Actuator) et d'un « ping » à MongoDB.
 * Les appels de la page de supervision elle-même ne sont pas comptés, pour ne pas fausser les requêtes par minute.
 */
@Component
@RequiredArgsConstructor
public class Echantillonneur {

    public static final Duration INTERVALLE = Duration.ofSeconds(10);
    private static final int NOMBRE_MAXIMUM = (int) (Duration.ofHours(1).toSeconds() / INTERVALLE.toSeconds());
    private static final String ADRESSE_SUPERVISION = "/api/administration/supervision";

    /**
     * Une mesure.
     *
     * @param processeur            utilisation du processeur par le serveur d'application (%)
     * @param memoireUtiliseeMo     mémoire utilisée par le serveur d'application (Mo)
     * @param requetesParMinute     requêtes reçues, ramenées à la minute
     * @param tempsReponseMoyenMs   temps de réponse moyen (ms), ou null s'il n'y a eu aucune requête
     * @param latenceBaseMs         temps de réponse de MongoDB à un « ping » (ms), ou null si la base ne répond pas
     */
    public record Echantillon(Instant moment, double processeur, double memoireUtiliseeMo, double requetesParMinute,
            Double tempsReponseMoyenMs, Double latenceBaseMs) {
    }

    private final MeterRegistry mesures;
    private final MongoTemplate mongoTemplate;
    private final Clock horloge;

    private final Deque<Echantillon> historique = new ArrayDeque<>();
    private long requetesPrecedentes;
    private double dureeTotalePrecedenteMs;

    @Scheduled(fixedRate = 10_000, initialDelay = 1_000)
    public synchronized void releverMesures() {
        long requetes = 0;
        double dureeTotaleMs = 0;
        for (Timer minuteur : mesures.find("http.server.requests").timers()) {
            String adresse = minuteur.getId().getTag("uri");
            if (adresse == null || !adresse.startsWith(ADRESSE_SUPERVISION)) {
                requetes += minuteur.count();
                dureeTotaleMs += minuteur.totalTime(TimeUnit.MILLISECONDS);
            }
        }
        long nouvellesRequetes = Math.max(0, requetes - requetesPrecedentes);
        double nouvelleDureeMs = Math.max(0, dureeTotaleMs - dureeTotalePrecedenteMs);
        requetesPrecedentes = requetes;
        dureeTotalePrecedenteMs = dureeTotaleMs;

        Echantillon echantillon = new Echantillon(
                horloge.instant(),
                arrondir(jauge("process.cpu.usage") * 100),
                arrondir(memoireUtiliseeOctets() / 1_048_576),
                arrondir(nouvellesRequetes * 60.0 / INTERVALLE.toSeconds()),
                nouvellesRequetes == 0 ? null : arrondir(nouvelleDureeMs / nouvellesRequetes),
                mesurerLatenceBase());

        historique.addLast(echantillon);
        while (historique.size() > NOMBRE_MAXIMUM) {
            historique.removeFirst();
        }
    }

    /** Les mesures de la dernière heure, de la plus ancienne à la plus récente. */
    public synchronized List<Echantillon> historique() {
        return new ArrayList<>(historique);
    }

    /** Temps de réponse de MongoDB à une commande « ping », ou null si la base ne répond pas. */
    public Double mesurerLatenceBase() {
        long debut = System.nanoTime();
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return arrondir((System.nanoTime() - debut) / 1_000_000.0);
        } catch (RuntimeException erreur) {
            return null;
        }
    }

    /** Mémoire « tas » utilisée par la machine Java (somme de ses zones). */
    public double memoireUtiliseeOctets() {
        return mesures.find("jvm.memory.used").tag("area", "heap").gauges().stream().mapToDouble(Gauge::value).sum();
    }

    public double memoireMaximaleOctets() {
        return mesures.find("jvm.memory.max").tag("area", "heap").gauges().stream()
                .mapToDouble(Gauge::value).filter(valeur -> valeur > 0).sum();
    }

    private double jauge(String nom) {
        Gauge jauge = mesures.find(nom).gauge();
        return jauge == null || Double.isNaN(jauge.value()) ? 0 : jauge.value();
    }

    private static double arrondir(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }
}
