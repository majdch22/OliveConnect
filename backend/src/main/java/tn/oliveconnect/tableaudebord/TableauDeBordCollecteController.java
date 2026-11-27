package tn.oliveconnect.tableaudebord;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** GET /api/collecte/tableau-de-bord : chiffres de l'accueil du responsable de collecte. */
@RestController
@PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
@RequiredArgsConstructor
public class TableauDeBordCollecteController {

    private final TableauDeBordCollecteService tableauDeBordService;

    @GetMapping("/api/collecte/tableau-de-bord")
    public TableauDeBordCollecteService.TableauDeBord donnees() {
        return tableauDeBordService.donnees();
    }
}
