package tn.oliveconnect.alerte;

/** Nature d'un signalement fait par un agriculteur depuis son verger. */
public enum TypeAlerte {

    MATURITE("Maturité"),
    PLUIE("Pluie"),
    RAVAGEUR("Ravageur"),
    AUTRE("Autre problème");

    private final String libelle;

    TypeAlerte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
