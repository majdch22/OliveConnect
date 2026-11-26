package tn.oliveconnect.referentiel;

/**
 * Catégories de qualité de l'huile, déterminées par l'acidité mesurée au pressoir
 * (seuils de la norme du Conseil oléicole international). Liste fixe, non modifiable.
 */
public enum CategorieQualite {

    EXTRA_VIERGE("Extra vierge", "Acidité inférieure ou égale à 0,8 %", 0.8),
    VIERGE("Vierge", "Acidité inférieure ou égale à 2 %", 2.0),
    COURANTE("Courante", "Acidité inférieure ou égale à 3,3 %", 3.3),
    LAMPANTE("Lampante", "Acidité supérieure à 3,3 % : huile à raffiner", Double.MAX_VALUE);

    private final String libelle;
    private final String description;
    private final double aciditeMaximale;

    CategorieQualite(String libelle, String description, double aciditeMaximale) {
        this.libelle = libelle;
        this.description = description;
        this.aciditeMaximale = aciditeMaximale;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }

    /** Catégorie correspondant à une acidité mesurée (en %). */
    public static CategorieQualite pourAcidite(double acidite) {
        for (CategorieQualite categorie : values()) {
            if (acidite <= categorie.aciditeMaximale) {
                return categorie;
            }
        }
        return LAMPANTE;
    }
}
