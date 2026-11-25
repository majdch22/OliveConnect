package tn.oliveconnect.utilisateur;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Informations propres aux agriculteurs, rangées dans le compte sous « agriculteur ».
 * Absentes pour le personnel : l'héritage du diagramme de classes devient un sous-document facultatif.
 */
@Getter
@Setter
@NoArgsConstructor
public class InfosAgriculteur {

    /** Carte d'identité nationale : 8 chiffres, unique parmi les agriculteurs. */
    @Indexed(unique = true, sparse = true)
    private String cin;

    private Delegation delegation;

    /** Attribué par l'administrateur lors de la validation de l'inscription (ex. ADH-0042). */
    private String numeroAdherent;

    /** Date à laquelle l'agriculteur a accepté les conditions d'utilisation. */
    private Instant dateAcceptationConditions;
}
