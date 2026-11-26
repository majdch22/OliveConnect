package tn.oliveconnect.administration;

import java.util.List;

import tn.oliveconnect.utilisateur.Role;

/**
 * Chiffres affichés sur l'accueil de l'administration et dans le menu (badge des inscriptions en attente).
 *
 * @param parRole nombre de comptes actifs pour chaque rôle
 */
public record StatistiquesComptes(
        long inscriptionsEnAttente,
        long inscriptionsRefusees,
        long comptesActifs,
        long comptesDesactives,
        long totalComptes,
        List<NombreParRole> parRole) {

    public record NombreParRole(Role role, String libelle, long nombre) {
    }
}
