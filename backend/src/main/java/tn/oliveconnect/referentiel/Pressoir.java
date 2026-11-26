package tn.oliveconnect.referentiel;

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
 * Pressoir partenaire qui reçoit les olives collectées.
 * Collection MongoDB : « pressoirs ».
 */
@Document(collection = "pressoirs")
@Getter
@Setter
@NoArgsConstructor
public class Pressoir {

    @Id
    private String id;

    /** Code métier (PR-01…), utilisé par les fichiers d'import. */
    @Indexed(unique = true)
    private String code;

    private String nom;
    private Delegation delegation;

    /**
     * Position GeoJSON [longitude, latitude]. L'index « 2dsphere » permet de trouver le pressoir
     * le plus proche d'un verger.
     */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint localisation;

    /** Quantité d'olives que le pressoir peut traiter en une journée. */
    private int capaciteJournaliereKg;

    private String telephone;

    /** Un pressoir archivé n'est plus proposé, mais reste visible dans l'historique. */
    private boolean archive;

    @CreatedDate
    private Instant dateCreation;
}
