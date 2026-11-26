package tn.oliveconnect.connexion;

import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.InfosAgriculteur;
import tn.oliveconnect.utilisateur.PreferencesNotification;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Informations du compte connecté (sans mot de passe ni compteurs internes).
 * Le numéro d'adhérent et la délégation ne sont remplis que pour un agriculteur.
 */
public record ProfilReponse(
        String id,
        String prenom,
        String nom,
        String email,
        String telephone,
        Role role,
        String libelleRole,
        boolean motDePasseProvisoire,
        String numeroAdherent,
        Delegation delegation,
        String libelleDelegation,
        PreferencesNotification preferences,
        boolean visiteGuideeTerminee) {

    public static ProfilReponse depuis(Utilisateur utilisateur) {
        InfosAgriculteur agriculteur = utilisateur.getAgriculteur();
        Delegation delegation = agriculteur == null ? null : agriculteur.getDelegation();
        return new ProfilReponse(
                utilisateur.getId(),
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getRole(),
                utilisateur.getRole().getLibelle(),
                utilisateur.isMotDePasseProvisoire(),
                agriculteur == null ? null : agriculteur.getNumeroAdherent(),
                delegation,
                delegation == null ? null : delegation.getLibelle(),
                utilisateur.preferences(),
                utilisateur.isVisiteGuideeTerminee());
    }
}
