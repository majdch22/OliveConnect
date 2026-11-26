package tn.oliveconnect.referentiel;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Référentiels de la plateforme (réservés à l'administrateur).
 * GET  /api/administration/referentiels/varietes                     liste (recherche, archives, tri)
 * POST /api/administration/referentiels/varietes                     création
 * PUT  /api/administration/referentiels/varietes/{id}                modification
 * POST /api/administration/referentiels/varietes/{id}/archiver       archivage (ou /restaurer)
 * Mêmes adresses pour /pressoirs.
 * GET  /api/administration/referentiels/categories-qualite           liste fixe, en lecture seule
 */
@RestController
@RequestMapping("/api/administration/referentiels")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class ReferentielsController {

    private final VarieteService varieteService;
    private final PressoirService pressoirService;

    // --- Variétés ---

    @GetMapping("/varietes")
    public List<VarieteReponse> listerVarietes(
            @RequestParam(required = false) String texte,
            @RequestParam(defaultValue = "false") boolean avecArchives,
            @RequestParam(defaultValue = "NOM") VarieteService.Tri tri,
            @RequestParam(defaultValue = "ASC") Sort.Direction sens) {
        return varieteService.lister(texte, avecArchives, tri, sens);
    }

    @PostMapping("/varietes")
    @ResponseStatus(HttpStatus.CREATED)
    public VarieteReponse creerVariete(@Valid @RequestBody VarieteRequete requete) {
        return varieteService.creer(requete);
    }

    @PutMapping("/varietes/{id}")
    public VarieteReponse modifierVariete(@PathVariable String id, @Valid @RequestBody VarieteRequete requete) {
        return varieteService.modifier(id, requete);
    }

    @PostMapping("/varietes/{id}/archiver")
    public VarieteReponse archiverVariete(@PathVariable String id) {
        return varieteService.changerArchivage(id, true);
    }

    @PostMapping("/varietes/{id}/restaurer")
    public VarieteReponse restaurerVariete(@PathVariable String id) {
        return varieteService.changerArchivage(id, false);
    }

    // --- Pressoirs ---

    @GetMapping("/pressoirs")
    public List<PressoirReponse> listerPressoirs(
            @RequestParam(required = false) String texte,
            @RequestParam(defaultValue = "false") boolean avecArchives,
            @RequestParam(defaultValue = "NOM") PressoirService.Tri tri,
            @RequestParam(defaultValue = "ASC") Sort.Direction sens) {
        return pressoirService.lister(texte, avecArchives, tri, sens);
    }

    @PostMapping("/pressoirs")
    @ResponseStatus(HttpStatus.CREATED)
    public PressoirReponse creerPressoir(@Valid @RequestBody PressoirRequete requete) {
        return pressoirService.creer(requete);
    }

    @PutMapping("/pressoirs/{id}")
    public PressoirReponse modifierPressoir(@PathVariable String id, @Valid @RequestBody PressoirRequete requete) {
        return pressoirService.modifier(id, requete);
    }

    @PostMapping("/pressoirs/{id}/archiver")
    public PressoirReponse archiverPressoir(@PathVariable String id) {
        return pressoirService.changerArchivage(id, true);
    }

    @PostMapping("/pressoirs/{id}/restaurer")
    public PressoirReponse restaurerPressoir(@PathVariable String id) {
        return pressoirService.changerArchivage(id, false);
    }

    // --- Catégories de qualité (liste fixe) ---

    public record CategorieQualiteReponse(CategorieQualite code, String libelle, String description) {
    }

    @GetMapping("/categories-qualite")
    public List<CategorieQualiteReponse> categoriesQualite() {
        return Arrays.stream(CategorieQualite.values())
                .map(categorie -> new CategorieQualiteReponse(categorie, categorie.getLibelle(), categorie.getDescription()))
                .toList();
    }
}
