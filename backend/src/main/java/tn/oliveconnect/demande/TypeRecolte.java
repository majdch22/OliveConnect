package tn.oliveconnect.demande;

/** Nature d'une demande de récolte. Une demande urgente passe en priorité dans la planification. */
public enum TypeRecolte {

    PLANIFIEE("Planifiée"),
    URGENTE("Urgente");

    private final String libelle;

    TypeRecolte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
