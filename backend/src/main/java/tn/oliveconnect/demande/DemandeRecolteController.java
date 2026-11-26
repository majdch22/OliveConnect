package tn.oliveconnect.demande;

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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * Demandes de récolte de l'agriculteur connecté.
 * GET  /api/agriculteur/demandes                 mes demandes, des plus récentes aux plus anciennes
 * GET  /api/agriculteur/demandes/{id}            une demande et son historique
 * POST /api/agriculteur/demandes                 déclarer une récolte
 * PUT  /api/agriculteur/demandes/{id}            modifier (avant le démarrage de la collecte)
 * POST /api/agriculteur/demandes/{id}/annuler    annuler (avant le démarrage de la collecte), motif facultatif
 */
@RestController
@RequestMapping("/api/agriculteur/demandes")
@PreAuthorize("hasRole('AGRICULTEUR')")
@RequiredArgsConstructor
public class DemandeRecolteController {

    private final DemandeRecolteService demandeService;

    /** Motif facultatif d'une annulation. */
    public record AnnulationRequete(@Size(max = 300, message = "300 caractères au maximum.") String motif) {
    }

    @GetMapping
    public List<DemandeReponse> mesDemandes(@AuthenticationPrincipal Jwt agriculteur) {
        return demandeService.mesDemandes(agriculteur.getSubject());
    }

    @GetMapping("/{id}")
    public DemandeReponse consulter(@PathVariable String id, @AuthenticationPrincipal Jwt agriculteur) {
        return demandeService.consulter(id, agriculteur.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeReponse declarer(@Valid @RequestBody DemandeRequete requete, @AuthenticationPrincipal Jwt agriculteur) {
        return demandeService.declarer(requete, agriculteur.getSubject());
    }

    @PutMapping("/{id}")
    public DemandeReponse modifier(@PathVariable String id, @Valid @RequestBody DemandeRequete requete,
            @AuthenticationPrincipal Jwt agriculteur) {
        return demandeService.modifier(id, requete, agriculteur.getSubject());
    }

    @PostMapping("/{id}/annuler")
    public DemandeReponse annuler(@PathVariable String id, @Valid @RequestBody AnnulationRequete requete,
            @AuthenticationPrincipal Jwt agriculteur) {
        return demandeService.annuler(id, requete.motif(), agriculteur.getSubject());
    }
}
