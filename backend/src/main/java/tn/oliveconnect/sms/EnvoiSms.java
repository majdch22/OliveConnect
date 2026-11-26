package tn.oliveconnect.sms;

/**
 * Envoi d'un SMS (décision 17).
 * Pour l'instant une seule version, simulée ({@link SmsSimule}) ; un vrai fournisseur pourra la remplacer
 * sans toucher au reste du code, qui ne connaît que cette interface.
 */
public interface EnvoiSms {

    /**
     * @param telephone numéro au format +216XXXXXXXX
     * @param texte     contenu du message
     * @param motif     raison de l'envoi, affichée dans la liste « Messages envoyés » de l'administrateur
     */
    void envoyer(String telephone, String texte, MotifSms motif);
}
