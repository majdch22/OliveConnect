package tn.oliveconnect.administration;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;

/**
 * Traitement des inscriptions d'agriculteurs (réservé à l'administrateur).
 * GET  /api/administration/inscriptions?etat=EN_ATTENTE|ACCEPTEE|REFUSEE   liste filtrée, triée et paginée
 * POST /api/administration/inscriptions/{id}/accepter                      active le compte, attribue le numéro d'adhérent
 * POST /api/administration/inscriptions/{id}/refuser                       refuse, avec un motif
 */
@RestController
@RequestMapping("/api/administration/inscriptions")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class AdministrationInscriptionsController {

    private final RechercheComptesService rechercheComptesService;
    private final ValidationInscriptionService validationInscriptionService;

    @GetMapping
    public PageResultat<CompteAdministration> lister(
            @RequestParam(defaultValue = "EN_ATTENTE") EtatInscription etat,
            @RequestParam(required = false) String texte,
            @RequestParam(required = false) Delegation delegation,
            @RequestParam(defaultValue = "DATE_CREATION") TriComptes tri,
            @RequestParam(defaultValue = "DESC") Sort.Direction sens,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        CriteresRecherche criteres = new CriteresRecherche(texte, Set.of(Role.AGRICULTEUR), etat.getStatuts(),
                delegation, tri, sens, page, taille);
        return PageResultat.depuis(rechercheComptesService.rechercher(criteres), CompteAdministration::depuis);
    }

    @PostMapping("/{id}/accepter")
    public CompteAdministration accepter(@PathVariable String id, @AuthenticationPrincipal Jwt administrateur) {
        return validationInscriptionService.accepter(id, administrateur.getSubject());
    }

    @PostMapping("/{id}/refuser")
    public CompteAdministration refuser(@PathVariable String id, @Valid @RequestBody RefusInscriptionRequete requete,
            @AuthenticationPrincipal Jwt administrateur) {
        return validationInscriptionService.refuser(id, requete.motif(), administrateur.getSubject());
    }
}
