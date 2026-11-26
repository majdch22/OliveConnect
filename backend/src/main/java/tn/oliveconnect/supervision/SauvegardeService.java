package tn.oliveconnect.supervision;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.configuration.OliveConnectProperties;

/**
 * Sauvegardes de la base MongoDB avec l'outil officiel mongodump.
 * - Automatique chaque nuit (planification dans application.yml), ou à la demande depuis la page de supervision.
 * - Une sauvegarde = un fichier compressé « oliveconnect-AAAAMMJJ-HHMMSS.archive.gz ».
 * - Les photos des signalements (fichiers, décision 15) sont copiées à côté, dans « oliveconnect-AAAAMMJJ-HHMMSS.photos.zip ».
 * - Seules les plus récentes sont gardées (7 par défaut) ; les plus anciennes sont supprimées avec leurs photos.
 * Restauration : mongorestore --gzip --archive=&lt;fichier&gt; (procédure décrite dans la documentation de déploiement).
 */
@Service
public class SauvegardeService {

    private static final Logger journal = LoggerFactory.getLogger(SauvegardeService.class);
    private static final String PREFIXE = "oliveconnect-";
    private static final String SUFFIXE = ".archive.gz";
    private static final String SUFFIXE_PHOTOS = ".photos.zip";
    private static final DateTimeFormatter FORMAT_NOM = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Une sauvegarde présente dans le dossier. */
    public record Sauvegarde(String nom, long tailleOctets, Instant date) {
    }

    private final OliveConnectProperties.Sauvegarde reglages;
    private final Path dossierPhotos;
    private final String adresseBase;
    private final Clock horloge;
    private final AtomicBoolean enCours = new AtomicBoolean(false);

    public SauvegardeService(OliveConnectProperties proprietes, @Value("${spring.mongodb.uri}") String adresseBase,
            Clock horloge) {
        this.reglages = proprietes.sauvegarde();
        this.dossierPhotos = Path.of(proprietes.photos().dossier()).toAbsolutePath().normalize();
        this.adresseBase = adresseBase;
        this.horloge = horloge;
    }

    /** Les sauvegardes présentes, de la plus récente à la plus ancienne. */
    public List<Sauvegarde> lister() {
        Path dossier = dossier();
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        try (Stream<Path> fichiers = Files.list(dossier)) {
            return fichiers
                    .filter(fichier -> fichier.getFileName().toString().startsWith(PREFIXE))
                    .filter(fichier -> fichier.getFileName().toString().endsWith(SUFFIXE))
                    .map(this::decrire)
                    .sorted(Comparator.comparing(Sauvegarde::date).reversed())
                    .toList();
        } catch (IOException erreur) {
            journal.warn("Impossible de lire le dossier des sauvegardes {} : {}", dossier, erreur.getMessage());
            return List.of();
        }
    }

    @Scheduled(cron = "${oliveconnect.sauvegarde.planification}", zone = "Africa/Tunis")
    public void sauvegardeAutomatique() {
        try {
            sauvegarder();
        } catch (ErreurMetierException erreur) {
            journal.error("La sauvegarde automatique a échoué : {}", erreur.getMessage());
        }
    }

    /** Lance mongodump et attend la fin (au plus deux minutes). Une seule sauvegarde à la fois. */
    public Sauvegarde sauvegarder() {
        if (!enCours.compareAndSet(false, true)) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "SAUVEGARDE_EN_COURS", "Une sauvegarde est déjà en cours.");
        }
        try {
            Files.createDirectories(dossier());
            String nom = PREFIXE + LocalDateTime.now(horloge).format(FORMAT_NOM) + SUFFIXE;
            Path fichier = dossier().resolve(nom);

            // Arguments passés un par un, sans interpréteur de commandes : rien ne peut s'y glisser
            Process processus = new ProcessBuilder(reglages.outilMongodump(), "--uri=" + adresseBase,
                    "--archive=" + fichier, "--gzip", "--quiet").redirectErrorStream(true).start();
            String sortie = lireSortie(processus.getInputStream());
            boolean termine = processus.waitFor(2, TimeUnit.MINUTES);
            if (!termine || processus.exitValue() != 0) {
                processus.destroyForcibly();
                Files.deleteIfExists(fichier);
                throw echec("mongodump s'est arrêté en erreur. " + sortie);
            }
            copierPhotos(dossier().resolve(nom.replace(SUFFIXE, SUFFIXE_PHOTOS)));
            supprimerLesPlusAnciennes();
            journal.info("Sauvegarde créée : {}", fichier);
            return decrire(fichier);
        } catch (IOException erreur) {
            throw echec("L'outil de sauvegarde est introuvable ou le dossier n'est pas accessible (" + erreur.getMessage() + ").");
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw echec("La sauvegarde a été interrompue.");
        } finally {
            enCours.set(false);
        }
    }

    private void supprimerLesPlusAnciennes() throws IOException {
        List<Sauvegarde> sauvegardes = lister();
        for (Sauvegarde ancienne : sauvegardes.subList(Math.min(reglages.conservation(), sauvegardes.size()), sauvegardes.size())) {
            Files.deleteIfExists(dossier().resolve(ancienne.nom()));
            Files.deleteIfExists(dossier().resolve(ancienne.nom().replace(SUFFIXE, SUFFIXE_PHOTOS)));
        }
    }

    /** Range toutes les photos des signalements dans une archive zip (rien à faire s'il n'y en a pas encore). */
    private void copierPhotos(Path archive) throws IOException {
        if (!Files.isDirectory(dossierPhotos)) {
            return;
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive));
                Stream<Path> photos = Files.list(dossierPhotos)) {
            for (Path photo : photos.filter(Files::isRegularFile).toList()) {
                zip.putNextEntry(new ZipEntry(photo.getFileName().toString()));
                Files.copy(photo, zip);
                zip.closeEntry();
            }
        }
    }

    private Sauvegarde decrire(Path fichier) {
        try {
            return new Sauvegarde(fichier.getFileName().toString(), Files.size(fichier),
                    Files.getLastModifiedTime(fichier).toInstant());
        } catch (IOException erreur) {
            return new Sauvegarde(fichier.getFileName().toString(), 0, Instant.EPOCH);
        }
    }

    public Path dossier() {
        return Path.of(reglages.dossier()).toAbsolutePath().normalize();
    }

    private static String lireSortie(InputStream flux) throws IOException {
        String sortie = new String(flux.readAllBytes(), StandardCharsets.UTF_8).trim();
        return sortie.length() > 300 ? sortie.substring(sortie.length() - 300) : sortie;
    }

    private static ErreurMetierException echec(String message) {
        journal.error("Sauvegarde impossible : {}", message);
        return new ErreurMetierException(HttpStatus.SERVICE_UNAVAILABLE, "SAUVEGARDE_IMPOSSIBLE", message);
    }
}
