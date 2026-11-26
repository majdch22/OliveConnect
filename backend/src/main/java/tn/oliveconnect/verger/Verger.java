package tn.oliveconnect.verger;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.oliveconnect.utilisateur.Delegation;

/**
 * Verger d'un agriculteur. Il n'appartient qu'à un seul agriculteur (décision R7).
 * Collection MongoDB : « vergers ».
 * - Référence vers l'agriculteur (agriculteurId) : un agriculteur peut avoir beaucoup de vergers.
 * - Le nom de la variété est copié (nomVariete) pour l'afficher sans relire la collection des variétés.
 */
@Document(collection = "vergers")
@Getter
@Setter
@NoArgsConstructor
public class Verger {

    @Id
    private String id;

    /** Code métier (V-0001…), utilisé par les fichiers d'import des capteurs. */
    @Indexed(unique = true)
    private String code;

    /** Propriétaire : seul lui voit et modifie ce verger. */
    @Indexed
    private String agriculteurId;

    private String nom;

    /** Position GeoJSON [longitude, latitude]. L'index « 2dsphere » sert la carte et la distance des signalements. */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint localisation;

    private Delegation delegation;
    private double superficieHa;
    private int nombreArbres;

    private String varieteId;
    private String nomVariete;

    /** Maturité en % mesurée par les capteurs (vide tant qu'aucune mesure n'est arrivée). */
    private Integer niveauMaturite;

    private StatutRecolte statutRecolte = StatutRecolte.NON_MUR;

    /** Un verger archivé n'est plus proposé, mais reste visible dans l'historique. */
    private boolean archive;

    @CreatedDate
    private Instant dateCreation;
}
