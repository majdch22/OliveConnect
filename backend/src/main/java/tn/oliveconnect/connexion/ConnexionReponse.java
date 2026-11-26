package tn.oliveconnect.connexion;

import java.time.Instant;

import tn.oliveconnect.utilisateur.Role;

/**
 * Réponse à une connexion réussie. L'interface garde le jeton et l'envoie avec chaque requête.
 *
 * @param id           identifiant du compte connecté
 * @param jeton        jeton d'accès signé
 * @param expiration   fin de validité du jeton
 * @param role         rôle du compte, qui décide de l'espace ouvert après connexion
 * @param libelleRole  rôle en toutes lettres
 * @param motDePasseProvisoire vrai si le mot de passe a été donné par l'administrateur : l'interface demande de le changer
 */
public record ConnexionReponse(
        String id,
        String jeton,
        Instant expiration,
        Role role,
        String libelleRole,
        String prenom,
        String nom,
        boolean motDePasseProvisoire) {
}
