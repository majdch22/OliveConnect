package tn.oliveconnect.ressource;

/** État actuel d'une ressource. Seule une ressource disponible peut être affectée à une tournée. */
public enum EtatRessource {

    DISPONIBLE("Disponible"),
    EN_PANNE("En panne"),
    EN_REVISION("En révision"),
    EN_CONGE("En congé");

    private final String libelle;

    EtatRessource(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
