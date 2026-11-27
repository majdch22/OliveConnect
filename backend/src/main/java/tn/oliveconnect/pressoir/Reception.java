package tn.oliveconnect.pressoir;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.oliveconnect.referentiel.CategorieQualite;

/**
 * Réception d'une benne au pressoir : pesée et qualité.
 * Collection MongoDB : « receptions ».
 * - Le poids de la benne est réparti entre les vergers de la tournée (sous-documents « parts », décision 11) :
 *   chaque kilo n'est compté qu'une fois dans les rendements.
 * - Index : (pressoir, jour) pour l'historique et les chiffres du jour ; (vergers) pour le rendement d'un verger ;
 *   (tournée, benne) unique : une benne n'est réceptionnée qu'une fois par tournée.
 */
@Document(collection = "receptions")
@CompoundIndexes({
        @CompoundIndex(name = "pressoir_jour", def = "{'pressoirId': 1, 'jour': -1, 'dateHeure': -1}"),
        @CompoundIndex(name = "tournee_benne", def = "{'tourneeId': 1, 'benneId': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Reception {

    @Id
    private String id;

    /** Numéro lisible (R-0001…). */
    @Indexed(unique = true)
    private String numero;

    private String pressoirId;
    private String tourneeId;
    private String numeroTournee;

    private String benneId;
    private String codeBenne;
    private int capaciteBenneKg;

    private int poidsNetKg;
    private double aciditePourcent;
    private CategorieQualite categorie;

    /** Poids reçu ÷ capacité de la benne, en %. */
    private int tauxRemplissage;

    /** Jour de la réception (fuseau Africa/Tunis) et heure exacte. */
    private LocalDate jour;
    private Instant dateHeure;

    private String auteurId;
    private String nomAuteur;
    private String commentaire;

    /** Part du poids attribuée à chaque verger de la tournée. */
    private List<Part> parts = new ArrayList<>();

    /** Vergers de la benne (index pour retrouver vite les réceptions d'un verger). */
    @Indexed
    private List<String> vergerIds = new ArrayList<>();

    public record Part(String demandeId, String vergerId, String nomVerger, String agriculteurId, int poidsKg) {
    }
}
