package tn.oliveconnect.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Transforme les exceptions en réponses au format {@link ReponseErreur}. */
@RestControllerAdvice
public class GestionnaireErreurs {

    /** Erreurs prévues par les règles métier. */
    @ExceptionHandler(ErreurMetierException.class)
    public ResponseEntity<ReponseErreur> erreurMetier(ErreurMetierException exception) {
        HttpStatus statut = exception.getStatut();
        ReponseErreur corps = new ReponseErreur(statut.value(), exception.getCode(), exception.getMessage(),
                exception.getChamps(), Instant.now());
        return ResponseEntity.status(statut).body(corps);
    }

    /** Champs du formulaire manquants ou invalides : on renvoie le message de chaque champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReponseErreur> donneesInvalides(MethodArgumentNotValidException exception) {
        Map<String, String> champs = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erreur -> champs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage()));
        ReponseErreur corps = new ReponseErreur(400, "DONNEES_INVALIDES",
                "Certains champs sont manquants ou incorrects.", champs, Instant.now());
        return ResponseEntity.badRequest().body(corps);
    }

    /** Corps de la requête absent ou mal formé. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReponseErreur> requeteIllisible(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ReponseErreur.de(400, "REQUETE_ILLISIBLE", "La requête envoyée est illisible."));
    }
}
