package tn.oliveconnect.security;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Fabrique les mots de passe provisoires donnés par l'administrateur.
 * Les caractères qui se confondent à la lecture (0/O, 1/l/I) sont exclus, car le mot de passe est dicté ou recopié.
 */
@Component
public class GenerateurMotDePasse {

    private static final String LETTRES = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHIFFRES = "23456789";
    private static final int LONGUEUR = 10;

    private final SecureRandom hasard = new SecureRandom();

    /** 10 caractères, toujours au moins une lettre et un chiffre (règle des mots de passe de la plateforme). */
    public String generer() {
        StringBuilder motDePasse = new StringBuilder();
        for (int i = 0; i < LONGUEUR - 2; i++) {
            motDePasse.append(LETTRES.charAt(hasard.nextInt(LETTRES.length())));
        }
        motDePasse.append(CHIFFRES.charAt(hasard.nextInt(CHIFFRES.length())));
        motDePasse.append(CHIFFRES.charAt(hasard.nextInt(CHIFFRES.length())));
        return motDePasse.toString();
    }
}
