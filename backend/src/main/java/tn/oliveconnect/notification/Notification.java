package tn.oliveconnect.notification;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message adressé à une personne. Collection MongoDB : « notifications ».
 * Une notification « pour un rôle » est copiée pour chaque personne du rôle : chacun la lit à son rythme.
 * - L'index (destinataire, date) sert la liste de la cloche, la plus récente d'abord.
 * - L'index TTL sur la date efface les notifications de plus de 90 jours.
 */
@Document(collection = "notifications")
@CompoundIndex(name = "destinataire_date", def = "{'destinataireId': 1, 'dateCreation': -1}")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    private String id;

    private String destinataireId;
    private CategorieNotification categorie;
    private String titre;
    private String message;

    /** Page de l'application à ouvrir en touchant la notification (facultatif). */
    private String lien;

    /** Vrai pour ce qui doit être vu en premier (demande urgente…). */
    private boolean prioritaire;

    private boolean lue;

    @Indexed(expireAfter = "90d")
    private Instant dateCreation;
}
