package tn.oliveconnect.tournee;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * Tournées du responsable de collecte (et de l'administrateur, qui peut tout faire).
 * GET  /api/collecte/planification/options?date=   chefs libres, ressources libres, équipes, pressoirs et leur capacité
 * POST /api/collecte/planification/verification    contrôle d'une tournée avant création (conflits, avertissements)
 * POST /api/collecte/tournees                      créer la tournée et prévenir agriculteurs et chef d'équipe
 * GET  /api/collecte/tournees?date=                tournées d'un jour (aujourd'hui par défaut)
 * GET  /api/collecte/tournees/{id}                 fiche d'une tournée
 * POST /api/collecte/tournees/{id}/annuler         annuler une tournée pas encore démarrée (motif obligatoire)
 */
@RestController
@RequestMapping("/api/collecte")
@PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
@RequiredArgsConstructor
public class TourneesController {

    private final PlanificationService planificationService;
    private final TourneeService tourneeService;
    private final Clock horloge;

    /** Motif obligatoire d'une annulation, envoyé aux agriculteurs et au chef d'équipe. */
    public record AnnulationRequete(
            @NotBlank(message = "Indiquez le motif de l'annulation.")
            @Size(max = 300, message = "300 caractères au maximum.")
            String motif) {
    }

    @GetMapping("/planification/options")
    public PlanificationService.Options options(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return planificationService.options(date);
    }

    @PostMapping("/planification/verification")
    public PlanificationService.Verification verifier(@Valid @RequestBody PlanificationRequete requete) {
        return planificationService.verifier(requete);
    }

    @PostMapping("/tournees")
    @ResponseStatus(HttpStatus.CREATED)
    public TourneeReponse creer(@Valid @RequestBody PlanificationRequete requete, @AuthenticationPrincipal Jwt auteur) {
        return planificationService.creer(requete, auteur.getSubject());
    }

    @GetMapping("/tournees")
    public List<TourneeReponse> duJour(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tourneeService.duJour(date == null ? LocalDate.now(horloge) : date);
    }

    @GetMapping("/tournees/{id}")
    public TourneeReponse consulter(@PathVariable String id) {
        return tourneeService.consulter(id);
    }

    @PostMapping("/tournees/{id}/annuler")
    public TourneeReponse annuler(@PathVariable String id, @Valid @RequestBody AnnulationRequete requete,
            @AuthenticationPrincipal Jwt auteur) {
        return tourneeService.annuler(id, requete.motif(), auteur.getSubject());
    }
}
