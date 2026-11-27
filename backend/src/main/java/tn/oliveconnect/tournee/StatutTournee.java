package tn.oliveconnect.tournee;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'une tournée de collecte :
 * PLANIFIEE → EN_COURS → LIVREE → CLOTUREE, ou ANNULEE tant qu'elle n'a pas démarré.
 * Le démarrage et la livraison sont faits par le chef d'équipe, la clôture par le pressoir (fonctionnalités suivantes).
 */
public enum StatutTournee {

    PLANIFIEE("Planifiée"),
    EN_COURS("En cours"),
    LIVREE("Livrée"),
    CLOTUREE("Clôturée"),
    ANNULEE("Annulée");

    /** Tournées qui occupent leurs ressources, leur chef d'équipe et la capacité du pressoir ce jour-là. */
    public static final Set<StatutTournee> ACTIVES = EnumSet.of(PLANIFIEE, EN_COURS, LIVREE, CLOTUREE);

    private final String libelle;

    StatutTournee(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
