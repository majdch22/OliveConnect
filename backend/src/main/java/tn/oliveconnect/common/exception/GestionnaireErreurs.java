package tn.oliveconnect.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Transforme toutes les exceptions en réponses au format unique {@link ReponseErreur}.
 * Une erreur imprévue est écrite dans le journal du serveur mais n'expose jamais de détail technique à l'utilisateur.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger journal = LoggerFactory.getLogger(GestionnaireErreurs.class);

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

    /** Corps de la requête absent ou mal formé (par exemple une valeur inconnue dans une liste). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReponseErreur> requeteIllisible(HttpMessageNotReadableException exception) {
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_ILLISIBLE", "La requête envoyée est illisible.");
    }

    /** Paramètre d'adresse de mauvais type (ex. ?tri=INCONNU). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ReponseErreur> parametreInvalide(MethodArgumentTypeMismatchException exception) {
        return reponse(HttpStatus.BAD_REQUEST, "PARAMETRE_INVALIDE",
                "La valeur du paramètre « " + exception.getName() + " » n'est pas reconnue.");
    }

    /** Deux personnes ont modifié le même document en même temps : la seconde doit recharger. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ReponseErreur> modificationConcurrente(OptimisticLockingFailureException exception) {
        return reponse(HttpStatus.CONFLICT, "MODIFIE_PAR_QUELQU_UN_D_AUTRE",
                "Ces informations viennent d'être modifiées par quelqu'un d'autre. Rechargez la page puis recommencez.");
    }

    /** Rôle insuffisant (@PreAuthorize). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ReponseErreur> accesRefuse(AccessDeniedException exception) {
        return reponse(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Votre profil ne permet pas cette action.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ReponseErreur> adresseInconnue(NoResourceFoundException exception) {
        return reponse(HttpStatus.NOT_FOUND, "ADRESSE_INCONNUE", "Cette adresse n'existe pas.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ReponseErreur> methodeNonPermise(HttpRequestMethodNotSupportedException exception) {
        return reponse(HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_PERMISE", "Cette action n'est pas permise à cette adresse.");
    }

    /** Fichier envoyé trop gros (photo de plus de 5 Mo) : refusé avant même d'arriver au service. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ReponseErreur> fichierTropGros(MaxUploadSizeExceededException exception) {
        return reponse(HttpStatus.BAD_REQUEST, "PHOTO_REFUSEE", "La photo dépasse 5 Mo.");
    }

    /** Toute autre erreur : écrite dans le journal, message neutre pour l'utilisateur. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> erreurImprevue(Exception exception) {
        journal.error("Erreur imprévue", exception);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur inattendue est survenue. Réessayez dans un instant.");
    }

    private static ResponseEntity<ReponseErreur> reponse(HttpStatus statut, String code, String message) {
        return ResponseEntity.status(statut).body(ReponseErreur.de(statut.value(), code, message));
    }
}
