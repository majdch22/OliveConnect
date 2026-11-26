package tn.oliveconnect.motdepasse;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Code à 6 chiffres envoyé par SMS pour choisir un nouveau mot de passe (décision 10).
 * Collection MongoDB : « codes_reinitialisation ».
 * - Un seul code à la fois par compte (index unique) : en demander un nouveau remplace l'ancien.
 * - Index TTL : MongoDB efface le code 10 minutes après sa création. Le passage du nettoyage pouvant prendre
 *   jusqu'à une minute, l'expiration est aussi vérifiée par le code.
 * - Le code est chiffré avec BCrypt, comme un mot de passe : lire la base ne suffit pas à le connaître.
 */
@Document(collection = "codes_reinitialisation")
@Getter
@Setter
@NoArgsConstructor
public class CodeReinitialisation {

    /** Durée de validité d'un code. */
    public static final String DUREE_VALIDITE = "10m";

    @Id
    private String id;

    @Indexed(unique = true)
    private String utilisateurId;

    private String codeChiffre;

    /** Essais encore permis ; à zéro, le code est annulé et il faut en demander un nouveau. */
    private int essaisRestants;

    @Indexed(expireAfter = DUREE_VALIDITE)
    private Instant dateCreation;
}
