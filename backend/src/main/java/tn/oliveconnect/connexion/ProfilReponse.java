package tn.oliveconnect.connexion;

import tn.oliveconnect.utilisateur.InfosAgriculteur;
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
        String libelleDelegation) {

    public static ProfilReponse depuis(Utilisateur utilisateur) {
        InfosAgriculteur agriculteur = utilisateur.getAgriculteur();
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
                agriculteur == null || agriculteur.getDelegation() == null ? null : agriculteur.getDelegation().getLibelle());
    }
}
