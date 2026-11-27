package tn.oliveconnect.demande;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.oliveconnect.referentiel.CategorieQualite;

/**
 * Demande de récolte déclarée par un agriculteur pour l'un de ses vergers.
 * Collection MongoDB : « demandes_recolte ».
 * - L'historique des changements d'état est rangé dans la demande (sous-documents) : il est toujours lu avec elle.
 * - Le nom du verger est copié pour l'afficher sans relire la collection des vergers.
 * - Le numéro de version empêche deux personnes d'écraser mutuellement leurs modifications.
 */
@Document(collection = "demandes_recolte")
@CompoundIndexes({
        @CompoundIndex(name = "agriculteur_statut", def = "{'agriculteurId': 1, 'statut': 1}"),
        @CompoundIndex(name = "verger_date", def = "{'vergerId': 1, 'dateSouhaitee': 1}"),
        // File de planification du responsable de collecte : par état, urgentes d'abord, puis par date souhaitée
        @CompoundIndex(name = "statut_type_date", def = "{'statut': 1, 'type': -1, 'dateSouhaitee': 1}")
})
@Getter
@Setter
@NoArgsConstructor
public class DemandeRecolte {

    @Id
    private String id;

    /** Version du document, augmentée à chaque enregistrement (verrou optimiste). */
    @Version
    private Long version;

    /** Numéro lisible (D-0001…). */
    @Indexed(unique = true)
    private String numero;

    private String agriculteurId;
    private String vergerId;
    private String nomVerger;

    private TypeRecolte type;
    private LocalDate dateSouhaitee;
    private int quantiteEstimeeKg;
    private String commentaire;

    private StatutDemande statut;
    private Instant dateSoumission;
    private String motifAnnulation;

    /** Motif donné par le responsable de collecte quand il rejette la demande (envoyé à l'agriculteur). */
    private String motifRejet;

    /** Tournée qui collecte ce verger, une fois la demande planifiée (vide sinon). */
    private String tourneeId;
    private String numeroTournee;

    /** Jour de la collecte choisi par la coopérative ; il peut différer de la date souhaitée. */
    private LocalDate datePlanifiee;

    /** Résultat au pressoir, rempli à la clôture de la tournée : poids reçu pour ce verger, acidité, catégorie. */
    private Integer poidsRecuKg;
    private Double aciditeMoyenne;
    private CategorieQualite categorieQualite;

    /** Vrai si la demande a été créée par un responsable de collecte (à partir d'un signalement), et non par l'agriculteur. */
    private boolean creeeParLaCooperative;

    /** Chaque changement d'état : ancien, nouveau, date, auteur. */
    private List<ChangementStatut> historique = new ArrayList<>();

    /** Un changement d'état, gardé à vie dans la demande. */
    public record ChangementStatut(StatutDemande ancien, StatutDemande nouveau, Instant date, String auteurId,
            String commentaire) {
    }

    /** Change l'état et l'inscrit dans l'historique. */
    public void changerStatut(StatutDemande nouveau, Instant date, String auteurId, String commentaire) {
        historique.add(new ChangementStatut(statut, nouveau, date, auteurId, commentaire));
        statut = nouveau;
    }

    /** Sort la demande de sa tournée (tournée annulée, demande annulée ou déplacée par l'agriculteur). */
    public void retirerDeLaTournee() {
        tourneeId = null;
        numeroTournee = null;
        datePlanifiee = null;
    }

    public boolean estModifiable() {
        return StatutDemande.MODIFIABLES.contains(statut);
    }
}
