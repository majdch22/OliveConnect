package tn.oliveconnect.connexion;

import tn.oliveconnect.utilisateur.Role;

/**
 * Les deux pages de connexion. L'administrateur a sa propre page à une adresse séparée ;
 * chaque page refuse les comptes qui ne lui correspondent pas.
 */
public enum EspaceConnexion {

    /** Page /connexion : agriculteurs et personnel de la coopérative. */
    UTILISATEURS,

    /** Page /administration/connexion : administrateurs uniquement. */
    ADMINISTRATION;

    public boolean accepte(Role role) {
        boolean estAdministrateur = role == Role.ADMINISTRATEUR;
        return this == ADMINISTRATION ? estAdministrateur : !estAdministrateur;
    }
}
