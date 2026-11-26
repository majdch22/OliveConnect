package tn.oliveconnect.notification;

import java.time.Instant;

/** Une notification telle qu'affichée (liste et message en temps réel). */
public record NotificationReponse(
        String id,
        CategorieNotification categorie,
        String libelleCategorie,
        String titre,
        String message,
        String lien,
        boolean prioritaire,
        boolean lue,
        Instant dateCreation) {

    public static NotificationReponse depuis(Notification notification) {
        return new NotificationReponse(notification.getId(), notification.getCategorie(),
                notification.getCategorie().getLibelle(), notification.getTitre(), notification.getMessage(),
                notification.getLien(), notification.isPrioritaire(), notification.isLue(), notification.getDateCreation());
    }
}
