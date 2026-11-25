package tn.oliveconnect.connexion;

import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.Utilisateur;

/** Informations du compte connecté (sans mot de passe ni compteurs internes). */
public record ProfilReponse(
        String id,
        String prenom,
        String nom,
        String email,
        String telephone,
        Role role,
        String libelleRole) {

    public static ProfilReponse depuis(Utilisateur utilisateur) {
        return new ProfilReponse(
                utilisateur.getId(),
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getRole(),
                utilisateur.getRole().getLibelle());
    }
}
