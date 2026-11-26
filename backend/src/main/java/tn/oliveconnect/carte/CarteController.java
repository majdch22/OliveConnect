package tn.oliveconnect.carte;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** GET /api/administration/carte : pressoirs et agriculteurs par délégation, pour la carte (réservé à l'administrateur). */
@RestController
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class CarteController {

    private final CarteService carteService;

    @GetMapping("/api/administration/carte")
    public CarteService.DonneesCarte donnees() {
        return carteService.donnees();
    }
}
