package tn.oliveconnect.demande;

import java.util.Set;

/**
 * Cycle de vie d'une demande de récolte :
 * SOUMISE → VALIDEE → PLANIFIEE → EN_COURS → TERMINEE, avec REJETEE (par le responsable de collecte)
 * et ANNULEE (par l'agriculteur, tant que la collecte n'a pas démarré).
 */
public enum StatutDemande {

    SOUMISE("Soumise"),
    VALIDEE("Validée"),
    PLANIFIEE("Planifiée"),
    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    REJETEE("Rejetée"),
    ANNULEE("Annulée");

    /** Demandes encore « vivantes » : une seule par verger et par date. */
    public static final Set<StatutDemande> ACTIFS = Set.of(SOUMISE, VALIDEE, PLANIFIEE, EN_COURS);

    /** L'agriculteur peut modifier ou annuler sa demande tant que la collecte n'a pas démarré. */
    public static final Set<StatutDemande> MODIFIABLES = Set.of(SOUMISE, VALIDEE, PLANIFIEE);

    private final String libelle;

    StatutDemande(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
