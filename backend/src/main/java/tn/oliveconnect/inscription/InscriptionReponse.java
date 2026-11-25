package tn.oliveconnect.inscription;

/**
 * Réponse à une inscription enregistrée, affichée sur la page de confirmation.
 *
 * @param prenom           pour personnaliser le message
 * @param telephoneMasque  numéro partiellement caché (ex. +216 98 ••• 412), pour que l'agriculteur le reconnaisse
 */
public record InscriptionReponse(String prenom, String telephoneMasque) {
}
