package tn.oliveconnect.alerte;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Signalements de l'agriculteur connecté.
 * GET  /api/agriculteur/signalements    mes signalements, des plus récents aux plus anciens
 * POST /api/agriculteur/signalements    signaler maturité ou problème sur un de mes vergers
 */
@RestController
@RequestMapping("/api/agriculteur/signalements")
@PreAuthorize("hasRole('AGRICULTEUR')")
@RequiredArgsConstructor
public class SignalementController {

    private final AlerteService alerteService;

    @GetMapping
    public List<AlerteService.AlerteReponse> mesSignalements(@AuthenticationPrincipal Jwt agriculteur) {
        return alerteService.mesSignalements(agriculteur.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlerteService.AlerteReponse signaler(@Valid @RequestBody SignalementRequete requete,
            @AuthenticationPrincipal Jwt agriculteur) {
        return alerteService.signaler(requete, agriculteur.getSubject());
    }
}
