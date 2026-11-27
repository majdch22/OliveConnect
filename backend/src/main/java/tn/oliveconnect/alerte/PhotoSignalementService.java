package tn.oliveconnect.alerte;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.configuration.OliveConnectProperties;

/**
 * Photo jointe à un signalement (décision 15).
 * - Fichier rangé sur le serveur, dans le dossier des photos (inclus dans les sauvegardes), et non dans la base.
 * - Images seulement (JPEG, PNG ou WebP), 5 Mo au plus. Le type est reconnu aux premiers octets du fichier,
 *   pas à son nom ni à ce qu'annonce le navigateur, qui peuvent mentir.
 * - Le nom du fichier vient de l'identifiant du signalement, jamais de la personne : rien ne peut sortir du dossier.
 * - Une nouvelle photo remplace la précédente. Seul l'auteur du signalement peut l'envoyer et la voir.
 */
@Service
public class PhotoSignalementService {

    /** Formats acceptés, reconnus par leur signature (premiers octets du fichier). */
    private enum FormatImage {
        JPEG("jpg", MediaType.IMAGE_JPEG, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
        PNG("png", MediaType.IMAGE_PNG, new byte[] { (byte) 0x89, 'P', 'N', 'G' }),
        WEBP("webp", MediaType.parseMediaType("image/webp"), "RIFF".getBytes(StandardCharsets.US_ASCII));

        private final String extension;
        private final MediaType type;
        private final byte[] signature;

        FormatImage(String extension, MediaType type, byte[] signature) {
            this.extension = extension;
            this.type = type;
            this.signature = signature;
        }

        static FormatImage reconnaitre(byte[] debut) {
            for (FormatImage format : values()) {
                boolean signatureTrouvee = debut.length >= format.signature.length
                        && Arrays.equals(Arrays.copyOf(debut, format.signature.length), format.signature);
                // Un fichier WebP commence par « RIFF », puis contient « WEBP » à l'octet 8
                boolean webpComplet = format != WEBP || (debut.length >= 12 && new String(debut, 8, 4, StandardCharsets.US_ASCII).equals("WEBP"));
                if (signatureTrouvee && webpComplet) {
                    return format;
                }
            }
            return null;
        }

        static FormatImage depuisExtension(String nomFichier) {
            return Arrays.stream(values()).filter(format -> nomFichier.endsWith("." + format.extension)).findFirst().orElse(JPEG);
        }
    }

    /** Une photo prête à être renvoyée au navigateur. */
    public record Photo(Resource fichier, MediaType type) {
    }

    private final AlerteRepository alerteRepository;
    private final Path dossier;
    private final long tailleMaximum;

    public PhotoSignalementService(AlerteRepository alerteRepository, OliveConnectProperties proprietes) {
        this.alerteRepository = alerteRepository;
        this.dossier = Path.of(proprietes.photos().dossier()).toAbsolutePath().normalize();
        this.tailleMaximum = proprietes.photos().tailleMaximum();
    }

    public AlerteService.AlerteReponse enregistrer(String alerteId, MultipartFile envoi, String agriculteurId) {
        Alerte alerte = signalementDe(alerteId, agriculteurId);
        if (envoi == null || envoi.isEmpty()) {
            throw photoRefusee("Choisissez une photo.");
        }
        if (envoi.getSize() > tailleMaximum) {
            throw photoRefusee("La photo dépasse 5 Mo.");
        }
        try {
            byte[] debut;
            try (InputStream flux = envoi.getInputStream()) {
                debut = flux.readNBytes(12);
            }
            FormatImage format = FormatImage.reconnaitre(debut);
            if (format == null) {
                throw photoRefusee("Seules les photos JPEG, PNG ou WebP sont acceptées.");
            }
            Files.createDirectories(dossier);
            String nomFichier = alerte.getId() + "." + format.extension;
            try (InputStream flux = envoi.getInputStream()) {
                Files.copy(flux, dossier.resolve(nomFichier), StandardCopyOption.REPLACE_EXISTING);
            }
            supprimerAnciennePhoto(alerte, nomFichier);
            alerte.setPhoto(nomFichier);
            return AlerteService.AlerteReponse.depuis(alerteRepository.save(alerte));
        } catch (IOException erreur) {
            throw new ErreurMetierException(HttpStatus.SERVICE_UNAVAILABLE, "PHOTO_NON_ENREGISTREE",
                    "La photo n'a pas pu être enregistrée. Réessayez plus tard.");
        }
    }

    public Photo lire(String alerteId, String agriculteurId) {
        return photoDe(signalementDe(alerteId, agriculteurId));
    }

    /** Photo vue par le responsable de collecte qui traite le signalement (tous les signalements, sans contrôle d'auteur). */
    public Photo lirePourLaCooperative(String alerteId) {
        return photoDe(alerteRepository.findById(alerteId).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "SIGNALEMENT_INTROUVABLE", "Ce signalement n'existe pas.")));
    }

    private Photo photoDe(Alerte alerte) {
        Path fichier = alerte.getPhoto() == null ? null : dossier.resolve(alerte.getPhoto());
        if (fichier == null || !Files.isRegularFile(fichier)) {
            throw new ErreurMetierException(HttpStatus.NOT_FOUND, "PHOTO_INTROUVABLE", "Ce signalement n'a pas de photo.");
        }
        return new Photo(new FileSystemResource(fichier), FormatImage.depuisExtension(alerte.getPhoto()).type);
    }

    /** Si l'ancienne photo avait un autre format (donc un autre nom), elle est effacée. */
    private void supprimerAnciennePhoto(Alerte alerte, String nouveauNom) throws IOException {
        if (alerte.getPhoto() != null && !alerte.getPhoto().equals(nouveauNom)) {
            Files.deleteIfExists(dossier.resolve(alerte.getPhoto()));
        }
    }

    private Alerte signalementDe(String alerteId, String agriculteurId) {
        Alerte alerte = alerteRepository.findById(alerteId).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "SIGNALEMENT_INTROUVABLE", "Ce signalement n'existe pas."));
        if (!alerte.getAgriculteurId().equals(agriculteurId)) {
            throw new ErreurMetierException(HttpStatus.FORBIDDEN, "SIGNALEMENT_D_UN_AUTRE_AGRICULTEUR",
                    "Ce signalement ne vous appartient pas.");
        }
        return alerte;
    }

    private static ErreurMetierException photoRefusee(String message) {
        return new ErreurMetierException(HttpStatus.BAD_REQUEST, "PHOTO_REFUSEE", message,
                Map.of("photo", message));
    }
}
