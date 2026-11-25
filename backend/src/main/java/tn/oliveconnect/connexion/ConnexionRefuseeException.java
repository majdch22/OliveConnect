package tn.oliveconnect.connexion;

import org.springframework.http.HttpStatus;

import tn.oliveconnect.common.exception.ErreurMetierException;

/** Connexion refusée. Chaque méthode correspond à un motif affiché à l'utilisateur. */
public class ConnexionRefuseeException extends ErreurMetierException {

    private ConnexionRefuseeException(HttpStatus statut, String code, String message) {
        super(statut, code, message);
    }

    /** Même message si le compte n'existe pas ou si le mot de passe est faux, pour ne pas révéler quels comptes existent. */
    public static ConnexionRefuseeException identifiantsIncorrects(int tentativesRestantes) {
        String message = "Identifiant ou mot de passe incorrect.";
        if (tentativesRestantes > 0) {
            message += " Il vous reste " + tentativesRestantes + " tentative(s) avant blocage temporaire.";
        }
        return new ConnexionRefuseeException(HttpStatus.UNAUTHORIZED, "IDENTIFIANTS_INCORRECTS", message);
    }

    public static ConnexionRefuseeException compteBloque(long minutesRestantes) {
        return new ConnexionRefuseeException(HttpStatus.LOCKED, "COMPTE_BLOQUE",
                "Compte bloqué après plusieurs mots de passe incorrects. Réessayez dans "
                        + minutesRestantes + " minute(s).");
    }

    public static ConnexionRefuseeException compteEnAttenteValidation() {
        return new ConnexionRefuseeException(HttpStatus.FORBIDDEN, "COMPTE_EN_ATTENTE_VALIDATION",
                "Votre inscription est en cours de vérification par la coopérative.");
    }

    public static ConnexionRefuseeException inscriptionRefusee() {
        return new ConnexionRefuseeException(HttpStatus.FORBIDDEN, "INSCRIPTION_REFUSEE",
                "Votre inscription a été refusée. Contactez la coopérative.");
    }

    public static ConnexionRefuseeException compteDesactive() {
        return new ConnexionRefuseeException(HttpStatus.FORBIDDEN, "COMPTE_DESACTIVE",
                "Ce compte est désactivé. Contactez l'administrateur de la coopérative.");
    }

    public static ConnexionRefuseeException mauvaisEspace(EspaceConnexion espaceDemande) {
        String message = espaceDemande == EspaceConnexion.ADMINISTRATION
                ? "Cette page est réservée à l'administrateur."
                : "Les administrateurs se connectent depuis l'espace d'administration.";
        return new ConnexionRefuseeException(HttpStatus.FORBIDDEN, "MAUVAIS_ESPACE_CONNEXION", message);
    }
}
