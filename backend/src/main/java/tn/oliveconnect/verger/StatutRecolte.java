package tn.oliveconnect.verger;

/** Avancement de la récolte d'un verger pendant la campagne. */
public enum StatutRecolte {

    NON_MUR("Pas encore mûr"),
    MUR("Mûr"),
    EN_RECOLTE("En récolte"),
    RECOLTE("Récolté");

    private final String libelle;

    StatutRecolte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
