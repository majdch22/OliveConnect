package tn.oliveconnect.alerte;

import java.util.EnumSet;
import java.util.Set;

/** Traitement d'un signalement : ouvert, pris en charge par le responsable de collecte, puis résolu. */
public enum StatutAlerte {

    OUVERTE("Ouverte"),
    PRISE_EN_CHARGE("Prise en charge"),
    RESOLUE("Résolue");

    /** États où le signalement attend encore une réponse de la coopérative. */
    public static final Set<StatutAlerte> NON_RESOLUS = EnumSet.of(OUVERTE, PRISE_EN_CHARGE);

    private final String libelle;

    StatutAlerte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
