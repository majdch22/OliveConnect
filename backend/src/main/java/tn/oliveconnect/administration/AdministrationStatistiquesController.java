package tn.oliveconnect.administration;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** GET /api/administration/statistiques : chiffres de l'accueil de l'administration (réservé à l'administrateur). */
@RestController
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class AdministrationStatistiquesController {

    private final GestionComptesService gestionComptesService;

    @GetMapping("/api/administration/statistiques")
    public StatistiquesComptes statistiques() {
        return gestionComptesService.statistiques();
    }
}
