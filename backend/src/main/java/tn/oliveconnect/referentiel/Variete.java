package tn.oliveconnect.referentiel;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Variété d'olive (Chemlali, Chetoui…), référencée par les vergers.
 * Son rendement moyen sert à proposer une quantité estimée et à comparer les rendements réels.
 * Collection MongoDB : « varietes ».
 */
@Document(collection = "varietes")
@Getter
@Setter
@NoArgsConstructor
public class Variete {

    @Id
    private String id;

    /** Code métier (VAR-01…), utilisé par les fichiers d'import. */
    @Indexed(unique = true)
    private String code;

    @Indexed(unique = true)
    private String nom;

    /** Rendement moyen en kilogrammes d'olives par hectare. */
    private int rendementMoyenKgParHectare;

    /** Mois (1 à 12) du début et de la fin habituels de la récolte. */
    private int moisDebutRecolte;
    private int moisFinRecolte;

    /** Une variété archivée n'est plus proposée, mais reste visible dans l'historique. */
    private boolean archivee;

    @CreatedDate
    private Instant dateCreation;
}
