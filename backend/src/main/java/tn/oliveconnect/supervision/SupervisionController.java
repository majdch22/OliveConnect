package tn.oliveconnect.supervision;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Supervision du serveur (réservée à l'administrateur).
 * GET  /api/administration/supervision     état des composants, mesures, historique, journal, sauvegardes
 * GET  /api/administration/sauvegardes     liste des sauvegardes
 * POST /api/administration/sauvegardes     lance une sauvegarde immédiate
 */
@RestController
@RequestMapping("/api/administration")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class SupervisionController {

    private final SupervisionService supervisionService;
    private final SauvegardeService sauvegardeService;

    @GetMapping("/supervision")
    public SupervisionService.EtatServeur etat() {
        return supervisionService.etatActuel();
    }

    @GetMapping("/sauvegardes")
    public List<SauvegardeService.Sauvegarde> sauvegardes() {
        return sauvegardeService.lister();
    }

    @PostMapping("/sauvegardes")
    @ResponseStatus(HttpStatus.CREATED)
    public SauvegardeService.Sauvegarde sauvegarder() {
        return sauvegardeService.sauvegarder();
    }
}
