package tn.oliveconnect.administration;

import java.util.Set;

import org.springframework.data.domain.Sort;

import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/**
 * Filtres, tri et pagination d'une liste de comptes. Un filtre vide (null ou ensemble vide) n'est pas appliqué.
 *
 * @param texte      recherche libre : nom, prénom, e-mail, téléphone, carte d'identité, numéro d'adhérent
 * @param roles      rôles acceptés
 * @param statuts    états de compte acceptés
 * @param delegation délégation (agriculteurs seulement)
 * @param tri        colonne de tri
 * @param sens       croissant ou décroissant
 * @param page       numéro de page (0 pour la première)
 * @param taille     nombre de comptes par page (au plus {@value #TAILLE_MAXIMUM})
 */
public record CriteresRecherche(
        String texte,
        Set<Role> roles,
        Set<StatutCompte> statuts,
        Delegation delegation,
        TriComptes tri,
        Sort.Direction sens,
        int page,
        int taille) {

    public static final int TAILLE_MAXIMUM = 100;

    public CriteresRecherche {
        roles = roles == null ? Set.of() : roles;
        statuts = statuts == null ? Set.of() : statuts;
        tri = tri == null ? TriComptes.DATE_CREATION : tri;
        sens = sens == null ? Sort.Direction.DESC : sens;
        page = Math.max(page, 0);
        taille = Math.clamp(taille, 1, TAILLE_MAXIMUM);
    }
}
