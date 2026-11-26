package tn.oliveconnect.administration;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/**
 * Gestion des comptes (réservée à l'administrateur).
 * GET  /api/administration/utilisateurs                        liste filtrée, triée et paginée
 * GET  /api/administration/utilisateurs/{id}                   fiche d'un compte
 * POST /api/administration/utilisateurs                        création d'un compte du personnel
 * PUT  /api/administration/utilisateurs/{id}                   modification
 * POST /api/administration/utilisateurs/{id}/desactiver        coupe l'accès
 * POST /api/administration/utilisateurs/{id}/reactiver         rend l'accès
 * POST /api/administration/utilisateurs/{id}/mot-de-passe      nouveau mot de passe provisoire
 */
@RestController
@RequestMapping("/api/administration/utilisateurs")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class AdministrationComptesController {

    private final RechercheComptesService rechercheComptesService;
    private final GestionComptesService gestionComptesService;

    @GetMapping
    public PageResultat<CompteAdministration> lister(
            @RequestParam(required = false) String texte,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) StatutCompte statut,
            @RequestParam(required = false) Delegation delegation,
            @RequestParam(defaultValue = "DATE_CREATION") TriComptes tri,
            @RequestParam(defaultValue = "DESC") Sort.Direction sens,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        CriteresRecherche criteres = new CriteresRecherche(texte,
                role == null ? Set.of() : Set.of(role),
                statut == null ? Set.of() : Set.of(statut),
                delegation, tri, sens, page, taille);
        return PageResultat.depuis(rechercheComptesService.rechercher(criteres), CompteAdministration::depuis);
    }

    @GetMapping("/{id}")
    public CompteAdministration consulter(@PathVariable String id) {
        return gestionComptesService.consulter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompteEtMotDePasse creer(@Valid @RequestBody CompteRequete requete) {
        return gestionComptesService.creer(requete);
    }

    @PutMapping("/{id}")
    public CompteAdministration modifier(@PathVariable String id, @Valid @RequestBody CompteRequete requete,
            @AuthenticationPrincipal Jwt administrateur) {
        return gestionComptesService.modifier(id, requete, administrateur.getSubject());
    }

    @PostMapping("/{id}/desactiver")
    public CompteAdministration desactiver(@PathVariable String id, @AuthenticationPrincipal Jwt administrateur) {
        return gestionComptesService.desactiver(id, administrateur.getSubject());
    }

    @PostMapping("/{id}/reactiver")
    public CompteAdministration reactiver(@PathVariable String id) {
        return gestionComptesService.reactiver(id);
    }

    @PostMapping("/{id}/mot-de-passe")
    public CompteEtMotDePasse reinitialiserMotDePasse(@PathVariable String id) {
        return gestionComptesService.reinitialiserMotDePasse(id);
    }
}
