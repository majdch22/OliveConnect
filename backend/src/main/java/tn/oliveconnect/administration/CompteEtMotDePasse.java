package tn.oliveconnect.administration;

/**
 * Réponse à une création de compte ou à une réinitialisation de mot de passe.
 * Le mot de passe provisoire n'est montré qu'une seule fois, à l'administrateur, qui le transmet à la personne.
 * Il n'est enregistré que chiffré : personne ne pourra le relire ensuite.
 */
public record CompteEtMotDePasse(CompteAdministration compte, String motDePasseProvisoire) {
}
