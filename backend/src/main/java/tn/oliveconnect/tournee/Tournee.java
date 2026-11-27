package tn.oliveconnect.tournee;

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
import tn.oliveconnect.demande.TypeRecolte;
import tn.oliveconnect.ressource.TypeRessource;

/**
 * Tournée de collecte : un jour, un chef d'équipe, un pressoir de destination, les vergers à récolter (étapes)
 * et les ressources affectées (tracteurs, bennes, ouvriers, matériel).
 * Collection MongoDB : « tournees ».
 * - Étapes et affectations sont rangées dans la tournée (sous-documents) : elles sont toujours lues avec elle.
 * - Les noms (chef, pressoir, verger, agriculteur, ressource) sont copiés pour l'affichage sans relire les autres collections.
 * - Index : (date, statut) pour la liste du jour ; (ressource, date) pour vérifier qu'une ressource n'est prise
 *   qu'une fois par jour ; (chef, date) pour la même règle sur les chefs d'équipe.
 * - Le numéro de version empêche deux responsables d'écraser mutuellement leurs modifications.
 */
@Document(collection = "tournees")
@CompoundIndexes({
        @CompoundIndex(name = "date_statut", def = "{'date': 1, 'statut': 1}"),
        @CompoundIndex(name = "ressource_date", def = "{'affectations.ressourceId': 1, 'date': 1}"),
        @CompoundIndex(name = "chef_date", def = "{'chefEquipeId': 1, 'date': -1}")
})
@Getter
@Setter
@NoArgsConstructor
public class Tournee {

    @Id
    private String id;

    @Version
    private Long version;

    /** Numéro lisible (T-0001…). */
    @Indexed(unique = true)
    private String numero;

    private LocalDate date;
    private StatutTournee statut;

    private String chefEquipeId;
    private String nomChefEquipe;

    private String pressoirId;
    private String nomPressoir;

    private List<Etape> etapes = new ArrayList<>();
    private List<Affectation> affectations = new ArrayList<>();

    /** Consigne laissée au chef d'équipe (accès, horaires…). */
    private String commentaire;
    private String motifAnnulation;

    private String createurId;
    private Instant dateCreation;

    private List<ChangementStatut> historique = new ArrayList<>();

    /** Un verger à récolter pendant la tournée, dans l'ordre de passage. */
    public record Etape(int ordre, String demandeId, String numeroDemande, String vergerId, String nomVerger,
            String agriculteurId, String nomAgriculteur, TypeRecolte type, int quantiteEstimeeKg) {

        public Etape avecQuantite(int quantite) {
            return new Etape(ordre, demandeId, numeroDemande, vergerId, nomVerger, agriculteurId, nomAgriculteur, type, quantite);
        }

        public Etape avecOrdre(int nouvelOrdre) {
            return new Etape(nouvelOrdre, demandeId, numeroDemande, vergerId, nomVerger, agriculteurId, nomAgriculteur, type,
                    quantiteEstimeeKg);
        }
    }

    /** Une ressource affectée à la tournée ; la capacité n'est remplie que pour une benne. */
    public record Affectation(String ressourceId, TypeRessource type, String code, String nom, Integer capaciteKg,
            String nomEquipe) {
    }

    /** Un changement d'état, gardé à vie dans la tournée. */
    public record ChangementStatut(StatutTournee ancien, StatutTournee nouveau, Instant date, String auteurId,
            String commentaire) {
    }

    public void changerStatut(StatutTournee nouveau, Instant date, String auteurId, String commentaire) {
        historique.add(new ChangementStatut(statut, nouveau, date, auteurId, commentaire));
        statut = nouveau;
    }

    public int quantiteEstimeeKg() {
        return etapes.stream().mapToInt(Etape::quantiteEstimeeKg).sum();
    }

    public int capaciteBennesKg() {
        return affectations.stream().filter(affectation -> affectation.type() == TypeRessource.BENNE)
                .mapToInt(affectation -> affectation.capaciteKg() == null ? 0 : affectation.capaciteKg()).sum();
    }
}
