package tn.oliveconnect.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Erreur prévue par une règle métier (et non une panne).
 * Elle porte le code de statut à renvoyer, un code lisible par l'interface
 * et, si besoin, le message à afficher sous chaque champ du formulaire concerné.
 */
public class ErreurMetierException extends RuntimeException {

    private final HttpStatus statut;
    private final String code;
    private final Map<String, String> champs;

    public ErreurMetierException(HttpStatus statut, String code, String message) {
        this(statut, code, message, Map.of());
    }

    public ErreurMetierException(HttpStatus statut, String code, String message, Map<String, String> champs) {
        super(message);
        this.statut = statut;
        this.code = code;
        this.champs = Map.copyOf(champs);
    }

    public HttpStatus getStatut() {
        return statut;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getChamps() {
        return champs;
    }
}
