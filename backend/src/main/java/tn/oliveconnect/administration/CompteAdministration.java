package tn.oliveconnect.administration;

import java.time.Instant;

import tn.oliveconnect.utilisateur.InfosAgriculteur;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Un compte tel que l'administrateur le voit dans ses listes et ses fiches.
 * Jamais de mot de passe ni de compteur interne. Les champs propres aux agriculteurs sont vides pour le personnel.
 */
public record CompteAdministration(
        String id,
        String nom,
        String prenom,
        String email,
        String telephone,
        Role role,
        String libelleRole,
        StatutCompte statut,
        boolean motDePasseProvisoire,
        Instant dateCreation,
        Instant derniereConnexion,
        String cin,
        String delegation,
        String libelleDelegation,
        String numeroAdherent,
        Instant dateTraitementInscription,
        String motifRefus,
        String pressoirId) {

    public static CompteAdministration depuis(Utilisateur compte) {
        InfosAgriculteur agriculteur = compte.getAgriculteur();
        boolean estAgriculteur = agriculteur != null;
        return new CompteAdministration(
                compte.getId(),
                compte.getNom(),
                compte.getPrenom(),
                compte.getEmail(),
                compte.getTelephone(),
                compte.getRole(),
                compte.getRole().getLibelle(),
                compte.getStatut(),
                compte.isMotDePasseProvisoire(),
                compte.getDateCreation(),
                compte.getDerniereConnexion(),
                estAgriculteur ? agriculteur.getCin() : null,
                estAgriculteur && agriculteur.getDelegation() != null ? agriculteur.getDelegation().name() : null,
                estAgriculteur && agriculteur.getDelegation() != null ? agriculteur.getDelegation().getLibelle() : null,
                estAgriculteur ? agriculteur.getNumeroAdherent() : null,
                estAgriculteur ? agriculteur.getDateTraitementInscription() : null,
                estAgriculteur ? agriculteur.getMotifRefus() : null,
                compte.getPressoirId());
    }
}
