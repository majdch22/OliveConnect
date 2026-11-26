package tn.oliveconnect.parametres;

import java.time.Duration;

/** Fréquences proposées pour les imports automatiques de données externes. */
public enum FrequenceImport {

    TOUTES_LES_5_MINUTES("Toutes les 5 minutes", Duration.ofMinutes(5)),
    TOUTES_LES_15_MINUTES("Toutes les 15 minutes", Duration.ofMinutes(15)),
    TOUTES_LES_30_MINUTES("Toutes les 30 minutes", Duration.ofMinutes(30)),
    TOUTES_LES_HEURES("Toutes les heures", Duration.ofHours(1)),
    CHAQUE_JOUR("Chaque jour à 2 h du matin", Duration.ofDays(1));

    private final String libelle;
    private final Duration intervalle;

    FrequenceImport(String libelle, Duration intervalle) {
        this.libelle = libelle;
        this.intervalle = intervalle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Temps entre deux imports, lu par le planificateur des imports. */
    public Duration getIntervalle() {
        return intervalle;
    }
}
