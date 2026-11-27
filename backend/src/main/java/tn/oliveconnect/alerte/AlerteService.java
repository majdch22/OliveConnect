package tn.oliveconnect.alerte;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.Distances;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerService;

/**
 * Signalements d'un agriculteur.
 * Le signalement porte sur l'un de ses vergers ; s'il est fait loin du verger (au-delà de la distance des paramètres),
 * il est enregistré quand même mais marqué « loin du verger » pour que le responsable de collecte le vérifie.
 * Les responsables de collecte sont prévenus en temps réel de chaque signalement.
 */
@Service
@RequiredArgsConstructor
public class AlerteService {

    /** Un signalement tel qu'affiché à l'agriculteur. */
    public record AlerteReponse(String id, String numero, String vergerId, String nomVerger, TypeAlerte type,
            String libelleType, double latitude, double longitude, int distanceAuVergerMetres, boolean loinDuVerger,
            String description, boolean avecPhoto, StatutAlerte statut, String libelleStatut, Instant dateCreation,
            String reponseCooperative) {

        public static AlerteReponse depuis(Alerte alerte) {
            // Dernier commentaire laissé par la coopérative : la réponse que l'agriculteur voit sous son signalement
            String reponse = alerte.getTraitements().reversed().stream().map(Alerte.Traitement::commentaire)
                    .filter(commentaire -> commentaire != null && !commentaire.isBlank()).findFirst().orElse(null);
            return new AlerteReponse(alerte.getId(), alerte.getNumero(), alerte.getVergerId(), alerte.getNomVerger(),
                    alerte.getType(), alerte.getType().getLibelle(), alerte.getPosition().getY(), alerte.getPosition().getX(),
                    alerte.getDistanceAuVergerMetres(), alerte.isLoinDuVerger(), alerte.getDescription(), alerte.getPhoto() != null,
                    alerte.getStatut(),
                    alerte.getStatut().getLibelle(), alerte.getDateCreation(), reponse);
        }
    }

    private final AlerteRepository alerteRepository;
    private final VergerService vergerService;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
    private final NotificationService notificationService;
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
        Alerte enregistree = alerteRepository.save(alerte);
        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE, NouvelleNotification.simple(CategorieNotification.ALERTE,
                enregistree.getType().getLibelle() + " · " + enregistree.getNomVerger() + " (" + enregistree.getNumero() + ")",
                enregistree.getDescription() + (enregistree.isLoinDuVerger()
                        ? " · Position à " + enregistree.getDistanceAuVergerMetres() + " m du verger, à vérifier." : ""),
                "/collecte"));
        return AlerteReponse.depuis(enregistree);
    }

    /** Distance à vol d'oiseau entre deux points GPS (formule de haversine), en mètres. */
    static double distanceMetres(double latitude1, double longitude1, double latitude2, double longitude2) {
        return Distances.metres(latitude1, longitude1, latitude2, longitude2);
    }
}
