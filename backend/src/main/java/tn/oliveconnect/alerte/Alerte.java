package tn.oliveconnect.alerte;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Signalement géolocalisé d'un agriculteur (maturité, pluie, ravageur…).
 * Collection MongoDB : « alertes ». L'index (statut, date) sert la file de traitement du responsable de collecte.
 * Le traitement par la coopérative (prise en charge, résolution, demande urgente créée) est rangé dans l'alerte
 * (sous-documents « traitements ») : il est toujours lu avec elle.
 */
@Document(collection = "alertes")
@CompoundIndex(name = "statut_date", def = "{'statut': 1, 'dateCreation': -1}")
@Getter
@Setter
@NoArgsConstructor
public class Alerte {

    @Id
    private String id;

    /** Numéro lisible (AL-0001…). */
    @Indexed(unique = true)
    private String numero;

    @Indexed
    private String agriculteurId;

    private String vergerId;
    private String nomVerger;

    private TypeAlerte type;

    /** Position où le signalement a été fait (GPS du téléphone ou point choisi sur la carte). */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint position;

    /** Distance entre la position du signalement et le verger, en mètres. */
    private int distanceAuVergerMetres;

    /** Vrai si cette distance dépasse la distance maximale des paramètres : le signalement est à vérifier. */
    private boolean loinDuVerger;

    private String description;

    /** Nom du fichier de la photo jointe (dossier des photos du serveur), ou null s'il n'y en a pas. */
    private String photo;

    private StatutAlerte statut;
    private Instant dateCreation;

    /** Date de la prise en charge par un responsable de collecte (sert au délai moyen de prise en charge). */
    private Instant datePriseEnCharge;
    private Instant dateResolution;

    /** Demande urgente créée par la coopérative à partir de ce signalement, s'il y en a une. */
    private String demandeUrgenteId;
    private String numeroDemandeUrgente;

    /** Chaque action de la coopérative : nouvel état, date, auteur et commentaire envoyé à l'agriculteur. */
    private List<Traitement> traitements = new ArrayList<>();

    /** Une action de la coopérative sur le signalement, gardée à vie. */
    public record Traitement(StatutAlerte statut, Instant date, String auteurId, String nomAuteur, String commentaire) {
    }

    /** Change l'état et inscrit l'action dans les traitements. */
    public void traiter(StatutAlerte nouveau, Instant date, String auteurId, String nomAuteur, String commentaire) {
        traitements.add(new Traitement(nouveau, date, auteurId, nomAuteur, commentaire));
        statut = nouveau;
    }
}
