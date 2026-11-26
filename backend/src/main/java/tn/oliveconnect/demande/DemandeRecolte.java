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
        @CompoundIndex(name = "verger_date", def = "{'vergerId': 1, 'dateSouhaitee': 1}")
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

    public boolean estModifiable() {
        return StatutDemande.MODIFIABLES.contains(statut);
    }
}
