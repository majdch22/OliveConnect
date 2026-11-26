package tn.oliveconnect.supervision;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.boot.SpringBootVersion;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.configuration.OliveConnectProperties;
import tn.oliveconnect.supervision.Echantillonneur.Echantillon;

/**
 * Rassemble l'état du serveur pour la page de supervision : santé de chaque composant, mesures actuelles,
 * historique de la dernière heure, derniers événements du journal et sauvegardes.
 *
 * Seuils d'alerte (état « Attention ») :
 * - mémoire du serveur d'application utilisée à plus de 85 % ;
 * - espace disque libre sous 10 % ;
 * - base de données qui met plus de 200 ms à répondre ;
 * - aucune sauvegarde, ou dernière sauvegarde de plus de 26 heures (la sauvegarde automatique est quotidienne).
 */
@Service
@RequiredArgsConstructor
public class SupervisionService {

    private static final double SEUIL_MEMOIRE = 0.85;
    private static final double SEUIL_DISQUE_LIBRE = 0.10;
    private static final double SEUIL_LATENCE_BASE_MS = 200;
    private static final Duration AGE_MAXIMUM_SAUVEGARDE = Duration.ofHours(26);

    /** Un composant surveillé et son état. */
    public record Composant(String nom, EtatComposant etat, String libelleEtat, String detail) {
        static Composant de(String nom, EtatComposant etat, String detail) {
            return new Composant(nom, etat, etat.getLibelle(), detail);
        }
    }

    /** Valeurs actuelles affichées en tête de page. */
    public record Indicateurs(double processeur, double memoireUtiliseeMo, double memoireMaximaleMo,
            double requetesParMinute, Double tempsReponseMoyenMs, int utilisateursActifs) {
    }

    public record EtatServeur(
            Instant genereLe,
            Instant demarreLe,
            long dureeFonctionnementSecondes,
            String adresseGrafana,
            List<Composant> composants,
            Indicateurs indicateurs,
            List<Echantillon> historique,
            List<JournalRecent.Evenement> evenements,
            List<SauvegardeService.Sauvegarde> sauvegardes) {
    }

    private final Echantillonneur echantillonneur;
    private final JournalRecent journalRecent;
    private final SuiviActivite suiviActivite;
    private final SauvegardeService sauvegardeService;
    private final MongoTemplate mongoTemplate;
    private final OliveConnectProperties proprietes;
    private final Clock horloge;

    public EtatServeur etatActuel() {
        Instant maintenant = horloge.instant();
        Instant demarrage = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
        List<Echantillon> historique = echantillonneur.historique();
        Echantillon dernier = historique.isEmpty() ? null : historique.getLast();
        List<SauvegardeService.Sauvegarde> sauvegardes = sauvegardeService.lister();

        double memoireUtilisee = echantillonneur.memoireUtiliseeOctets();
        double memoireMaximale = echantillonneur.memoireMaximaleOctets();

        List<Composant> composants = new ArrayList<>();
        composants.add(Composant.de("Serveur d'application", EtatComposant.OPERATIONNEL,
                "Spring Boot " + SpringBootVersion.getVersion() + " · Java " + Runtime.version().feature()
                        + " · en service depuis " + dureeLisible(Duration.between(demarrage, maintenant))));
        composants.add(etatBase());
        composants.add(etatMemoire(memoireUtilisee, memoireMaximale));
        composants.add(etatDisque());
        composants.add(etatSauvegardes(sauvegardes, maintenant));

        Indicateurs indicateurs = new Indicateurs(
                dernier == null ? 0 : dernier.processeur(),
                Math.round(memoireUtilisee / 1_048_576),
                Math.round(memoireMaximale / 1_048_576),
                dernier == null ? 0 : dernier.requetesParMinute(),
                dernier == null ? null : dernier.tempsReponseMoyenMs(),
                suiviActivite.nombreUtilisateursActifs());

        return new EtatServeur(maintenant, demarrage, Duration.between(demarrage, maintenant).toSeconds(),
                proprietes.supervision() == null ? "" : proprietes.supervision().adresseGrafana(),
                composants, indicateurs, historique, journalRecent.derniers(), sauvegardes);
    }

    private Composant etatBase() {
        Double latence = echantillonneur.mesurerLatenceBase();
        if (latence == null) {
            return Composant.de("Base de données MongoDB", EtatComposant.HORS_SERVICE, "La base ne répond pas.");
        }
        Document infos = mongoTemplate.executeCommand("{ buildInfo: 1 }");
        Document statistiques = mongoTemplate.executeCommand("{ dbStats: 1 }");
        String detail = "MongoDB " + infos.getString("version") + " · répond en " + latence.intValue() + " ms · "
                + statistiques.get("collections") + " collections · " + tailleLisible(statistiques.get("dataSize")) + " de données";
        return Composant.de("Base de données MongoDB",
                latence > SEUIL_LATENCE_BASE_MS ? EtatComposant.ATTENTION : EtatComposant.OPERATIONNEL, detail);
    }

    private static Composant etatMemoire(double utilisee, double maximale) {
        double part = maximale > 0 ? utilisee / maximale : 0;
        String detail = Math.round(utilisee / 1_048_576) + " Mo utilisés sur " + Math.round(maximale / 1_048_576)
                + " Mo (" + Math.round(part * 100) + " %)";
        return Composant.de("Mémoire du serveur d'application",
                part > SEUIL_MEMOIRE ? EtatComposant.ATTENTION : EtatComposant.OPERATIONNEL, detail);
    }

    private Composant etatDisque() {
        File disque = sauvegardeService.dossier().getRoot().toFile();
        double total = disque.getTotalSpace();
        double libre = disque.getUsableSpace();
        double partLibre = total > 0 ? libre / total : 0;
        String detail = Math.round(libre / 1_073_741_824) + " Go libres sur " + Math.round(total / 1_073_741_824)
                + " Go (" + Math.round(partLibre * 100) + " % libres)";
        return Composant.de("Espace disque",
                partLibre < SEUIL_DISQUE_LIBRE ? EtatComposant.ATTENTION : EtatComposant.OPERATIONNEL, detail);
    }

    private static Composant etatSauvegardes(List<SauvegardeService.Sauvegarde> sauvegardes, Instant maintenant) {
        if (sauvegardes.isEmpty()) {
            return Composant.de("Sauvegardes", EtatComposant.ATTENTION, "Aucune sauvegarde pour l'instant.");
        }
        Duration age = Duration.between(sauvegardes.getFirst().date(), maintenant);
        String detail = sauvegardes.size() + " sauvegarde(s) · la dernière date de " + dureeLisible(age);
        return Composant.de("Sauvegardes",
                age.compareTo(AGE_MAXIMUM_SAUVEGARDE) > 0 ? EtatComposant.ATTENTION : EtatComposant.OPERATIONNEL, detail);
    }

    /** « 3 j 4 h », « 2 h 15 min », « 12 min » ou « moins d'une minute ». */
    static String dureeLisible(Duration duree) {
        if (duree.toDays() > 0) {
            return duree.toDays() + " j " + duree.toHoursPart() + " h";
        }
        if (duree.toHours() > 0) {
            return duree.toHours() + " h " + duree.toMinutesPart() + " min";
        }
        return duree.toMinutes() < 1 ? "moins d'une minute" : duree.toMinutes() + " min";
    }

    /** Taille en octets écrite en Ko sous 1 Mo, en Mo au-delà. */
    static String tailleLisible(Object octets) {
        double taille = octets instanceof Number nombre ? nombre.doubleValue() : 0;
        return taille < 1_048_576
                ? Math.max(1, Math.round(taille / 1024)) + " Ko"
                : Math.round(taille / 1_048_576) + " Mo";
    }
}
