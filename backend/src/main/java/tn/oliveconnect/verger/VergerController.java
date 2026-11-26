package tn.oliveconnect.verger;

import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Vergers de l'agriculteur connecté. L'agriculteur est toujours celui du jeton d'accès, jamais une valeur envoyée.
 * GET  /api/agriculteur/vergers                     mes vergers
 * POST /api/agriculteur/vergers                     ajout
 * PUT  /api/agriculteur/vergers/{id}                modification
 * POST /api/agriculteur/vergers/{id}/archiver       archivage (ou /restaurer)
 * GET  /api/agriculteur/vergers/{id}/estimation     quantité attendue (kg), proposée dans « Déclarer une récolte »
 */
@RestController
@RequestMapping("/api/agriculteur/vergers")
@PreAuthorize("hasRole('AGRICULTEUR')")
@RequiredArgsConstructor
public class VergerController {

    private final VergerService vergerService;

    @GetMapping
    public List<VergerReponse> mesVergers(@AuthenticationPrincipal Jwt agriculteur) {
        return vergerService.mesVergers(agriculteur.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VergerReponse creer(@Valid @RequestBody VergerRequete requete, @AuthenticationPrincipal Jwt agriculteur) {
        return vergerService.creer(requete, agriculteur.getSubject());
    }

    @PutMapping("/{id}")
    public VergerReponse modifier(@PathVariable String id, @Valid @RequestBody VergerRequete requete,
            @AuthenticationPrincipal Jwt agriculteur) {
        return vergerService.modifier(id, requete, agriculteur.getSubject());
    }

    @PostMapping("/{id}/archiver")
    public VergerReponse archiver(@PathVariable String id, @AuthenticationPrincipal Jwt agriculteur) {
        return vergerService.changerArchivage(id, true, agriculteur.getSubject());
    }

    @PostMapping("/{id}/restaurer")
    public VergerReponse restaurer(@PathVariable String id, @AuthenticationPrincipal Jwt agriculteur) {
        return vergerService.changerArchivage(id, false, agriculteur.getSubject());
    }

    /** Réponse : { "quantiteKg": 13500 }. */
    public record Estimation(int quantiteKg) {
    }

    @GetMapping("/{id}/estimation")
    public Estimation estimation(@PathVariable String id, @AuthenticationPrincipal Jwt agriculteur) {
        return new Estimation(vergerService.estimationKg(id, agriculteur.getSubject()));
    }
}
