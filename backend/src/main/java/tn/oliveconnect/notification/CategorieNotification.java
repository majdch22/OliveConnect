package tn.oliveconnect.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Famille d'une notification : sert aux onglets de la page « Notifications ». */
@Getter
@RequiredArgsConstructor
public enum CategorieNotification {
    COLLECTE("Collectes"),
    ALERTE("Alertes"),
    COMPTE("Compte");

    private final String libelle;
}
