package tn.oliveconnect.carte;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * GET /api/collecte/carte?ouest=&sud=&est=&nord= : vergers, signalements non résolus et pressoirs pour la carte
 * du responsable de collecte. Sans zone, toute la région est renvoyée.
 */
@RestController
@PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
@RequiredArgsConstructor
public class CarteCollecteController {

    private final CarteCollecteService carteService;

    @GetMapping("/api/collecte/carte")
    public CarteCollecteService.DonneesCarteCollecte donnees(
            @RequestParam(required = false) Double ouest, @RequestParam(required = false) Double sud,
            @RequestParam(required = false) Double est, @RequestParam(required = false) Double nord) {
        boolean zoneComplete = ouest != null && sud != null && est != null && nord != null;
        return carteService.donnees(zoneComplete ? new CarteCollecteService.Zone(ouest, sud, est, nord) : null);
    }
}
