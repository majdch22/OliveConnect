package tn.oliveconnect.referentiel;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Listes de choix utiles à tous les comptes connectés (par exemple les variétés dans le formulaire d'un verger).
 * GET /api/listes/varietes : variétés actives, par ordre alphabétique.
 */
@RestController
@RequiredArgsConstructor
public class ListesController {

    private final VarieteService varieteService;

    @GetMapping("/api/listes/varietes")
    public List<VarieteReponse> varietes() {
        return varieteService.lister(null, false, VarieteService.Tri.NOM, Sort.Direction.ASC);
    }
}
