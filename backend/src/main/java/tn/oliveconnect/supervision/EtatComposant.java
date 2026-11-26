package tn.oliveconnect.supervision;

/** État d'un composant surveillé, affiché avec une icône et un libellé (jamais la couleur seule). */
public enum EtatComposant {

    OPERATIONNEL("Opérationnel"),
    ATTENTION("Attention"),
    HORS_SERVICE("Hors service");

    private final String libelle;

    EtatComposant(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
