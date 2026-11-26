package tn.oliveconnect.notification;

/**
 * Contenu d'une notification à envoyer.
 *
 * @param lien      page de l'application à ouvrir en touchant la notification, ou null
 * @param texteSms  texte d'un SMS à envoyer en plus, ou null pour ne pas envoyer de SMS
 */
public record NouvelleNotification(CategorieNotification categorie, String titre, String message, String lien,
        boolean prioritaire, String texteSms) {

    public static NouvelleNotification simple(CategorieNotification categorie, String titre, String message, String lien) {
        return new NouvelleNotification(categorie, titre, message, lien, false, null);
    }

    /** La même notification, marquée prioritaire (demande urgente…). */
    public NouvelleNotification enPriorite() {
        return new NouvelleNotification(categorie, titre, message, lien, true, texteSms);
    }

    /** La même notification, doublée d'un SMS. */
    public NouvelleNotification avecSms(String texte) {
        return new NouvelleNotification(categorie, titre, message, lien, prioritaire, texte);
    }
}
