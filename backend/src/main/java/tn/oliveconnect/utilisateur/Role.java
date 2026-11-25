package tn.oliveconnect.utilisateur;

/** Profils de la plateforme. Un compte a un seul rôle. */
public enum Role {

    AGRICULTEUR("Agriculteur"),
    RESPONSABLE_COLLECTE("Responsable de collecte"),
    CHEF_EQUIPE("Chef d'équipe de récolte"),
    RESPONSABLE_LOGISTIQUE("Responsable logistique"),
    RESPONSABLE_PRESSOIR("Responsable du pressoir"),
    ADMINISTRATEUR("Administrateur");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    /** Nom affiché à l'écran. */
    public String getLibelle() {
        return libelle;
    }
}
