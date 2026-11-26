package tn.oliveconnect.alerte;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerService;

/**
 * Signalements d'un agriculteur.
 * Le signalement porte sur l'un de ses vergers ; s'il est fait loin du verger (au-delà de la distance des paramètres),
 * il est enregistré quand même mais marqué « loin du verger » pour que le responsable de collecte le vérifie.
 */
@Service
@RequiredArgsConstructor
public class AlerteService {

    private static final double RAYON_TERRE_METRES = 6_371_000;

    /** Un signalement tel qu'affiché à l'agriculteur. */
    public record AlerteReponse(String id, String numero, String vergerId, String nomVerger, TypeAlerte type,
            String libelleType, double latitude, double longitude, int distanceAuVergerMetres, boolean loinDuVerger,
            String description, StatutAlerte statut, String libelleStatut, Instant dateCreation) {

        static AlerteReponse depuis(Alerte alerte) {
            return new AlerteReponse(alerte.getId(), alerte.getNumero(), alerte.getVergerId(), alerte.getNomVerger(),
                    alerte.getType(), alerte.getType().getLibelle(), alerte.getPosition().getY(), alerte.getPosition().getX(),
                    alerte.getDistanceAuVergerMetres(), alerte.isLoinDuVerger(), alerte.getDescription(), alerte.getStatut(),
                    alerte.getStatut().getLibelle(), alerte.getDateCreation());
        }
    }

    private final AlerteRepository alerteRepository;
    private final VergerService vergerService;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
    private final Clock horloge;

    public List<AlerteReponse> mesSignalements(String agriculteurId) {
        return alerteRepository.findByAgriculteurIdOrderByDateCreationDesc(agriculteurId).stream()
                .map(AlerteReponse::depuis).toList();
    }

    public AlerteReponse signaler(SignalementRequete requete, String agriculteurId) {
        Verger verger = vergerService.verifierProprietaire(requete.vergerId(), agriculteurId);
        if (verger.isArchive()) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES", "Ce verger est archivé.",
                    Map.of("vergerId", "Ce verger est archivé."));
        }
        int distance = (int) Math.round(distanceMetres(requete.latitude(), requete.longitude(),
                verger.getLocalisation().getY(), verger.getLocalisation().getX()));

        Alerte alerte = new Alerte();
        alerte.setNumero(compteurService.prochainCode("ALERTE", "AL-%04d"));
        alerte.setAgriculteurId(agriculteurId);
        alerte.setVergerId(verger.getId());
        alerte.setNomVerger(verger.getNom());
        alerte.setType(requete.type());
        // GeoJSON range les coordonnées dans l'ordre [longitude, latitude]
        alerte.setPosition(new GeoJsonPoint(requete.longitude(), requete.latitude()));
        alerte.setDistanceAuVergerMetres(distance);
        alerte.setLoinDuVerger(distance > parametresService.actuels().regles().distanceMaximaleSignalement());
        alerte.setDescription(requete.description().trim());
        alerte.setStatut(StatutAlerte.OUVERTE);
        alerte.setDateCreation(horloge.instant());
        return AlerteReponse.depuis(alerteRepository.save(alerte));
    }

    /** Distance à vol d'oiseau entre deux points GPS (formule de haversine), en mètres. */
    static double distanceMetres(double latitude1, double longitude1, double latitude2, double longitude2) {
        double ecartLatitude = Math.toRadians(latitude2 - latitude1);
        double ecartLongitude = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(ecartLatitude / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.pow(Math.sin(ecartLongitude / 2), 2);
        return 2 * RAYON_TERRE_METRES * Math.asin(Math.sqrt(a));
    }
}
