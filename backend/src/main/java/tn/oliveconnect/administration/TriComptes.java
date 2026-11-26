package tn.oliveconnect.administration;

/** Colonnes selon lesquelles l'administrateur peut trier les listes de comptes. */
public enum TriComptes {

    NOM("nom"),
    ROLE("role"),
    STATUT("statut"),
    DATE_CREATION("dateCreation"),
    DERNIERE_CONNEXION("derniereConnexion");

    /** Nom du champ dans la collection MongoDB. */
    private final String champ;

    TriComptes(String champ) {
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}
