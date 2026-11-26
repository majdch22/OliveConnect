package tn.oliveconnect.sms;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Trace d'un SMS simulé. Collection MongoDB : « messages_envoyes ».
 * L'index sur la date sert la liste de l'administrateur (plus récents d'abord) et efface les traces
 * au bout de 90 jours (index TTL) : la collection ne grossit pas sans fin.
 */
@Document(collection = "messages_envoyes")
public record MessageEnvoye(
        @Id String id,
        String telephone,
        String texte,
        MotifSms motif,
        @Indexed(expireAfter = "90d") Instant dateEnvoi) {
}
