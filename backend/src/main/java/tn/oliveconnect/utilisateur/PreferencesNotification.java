package tn.oliveconnect.utilisateur;

/**
 * Comment la personne veut être prévenue (page « Mon profil »).
 *
 * @param application recevoir les notifications dans l'application (cloche et messages en temps réel)
 * @param sms         recevoir aussi un SMS pour le suivi des collectes
 */
public record PreferencesNotification(boolean application, boolean sms) {

    /** Préférences d'un compte qui n'a encore rien choisi : tout est activé. */
    public static final PreferencesNotification PAR_DEFAUT = new PreferencesNotification(true, true);
}
