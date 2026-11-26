package tn.oliveconnect.alerte;

/** Traitement d'un signalement : ouvert, pris en charge par le responsable de collecte, puis résolu. */
public enum StatutAlerte {

    OUVERTE("Ouverte"),
    PRISE_EN_CHARGE("Prise en charge"),
    RESOLUE("Résolue");

    private final String libelle;

    StatutAlerte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
