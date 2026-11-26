package tn.oliveconnect.ressource;

/**
 * Types de ressources affectées aux tournées. Chaque type a son préfixe de code (TR-01, BN-01, OUV-001, MAT-01).
 */
public enum TypeRessource {

    TRACTEUR("Tracteur", "TR-%02d"),
    BENNE("Benne", "BN-%02d"),
    OUVRIER("Ouvrier saisonnier", "OUV-%03d"),
    MATERIEL("Matériel", "MAT-%02d");

    private final String libelle;
    private final String formatCode;

    TypeRessource(String libelle, String formatCode) {
        this.libelle = libelle;
        this.formatCode = formatCode;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Modèle du code, rempli avec le numéro suivant du compteur de ce type. */
    public String getFormatCode() {
        return formatCode;
    }
}
