package tn.oliveconnect.common.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Format unique des erreurs renvoyées par le serveur.
 *
 * @param statut     code de statut (400, 401, 403…)
 * @param code       motif lisible par l'interface (ex. IDENTIFIANTS_INCORRECTS)
 * @param message    phrase affichée à l'utilisateur
 * @param champs     erreurs par champ du formulaire (vide si aucune)
 * @param horodatage moment de l'erreur
 */
public record ReponseErreur(int statut, String code, String message, Map<String, String> champs, Instant horodatage) {

    public static ReponseErreur de(int statut, String code, String message) {
        return new ReponseErreur(statut, code, message, Map.of(), Instant.now());
    }
}
