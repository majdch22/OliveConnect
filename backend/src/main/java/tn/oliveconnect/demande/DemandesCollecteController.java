package tn.oliveconnect.demande;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;

/**
 * Demandes de récolte vues par le responsable de collecte (et l'administrateur, qui peut tout faire).
 * GET  /api/collecte/demandes                 file filtrée (état, type, texte), urgentes puis plus mûres d'abord, paginée
 * GET  /api/collecte/demandes/compteurs       nombre de demandes par état (onglets)
 * GET  /api/collecte/demandes/{id}            fiche : demande, verger, agriculteur, pressoir le plus proche
 * POST /api/collecte/demandes/{id}/valider    valider une demande soumise (commentaire facultatif)
 * POST /api/collecte/demandes/{id}/rejeter    rejeter une demande soumise ou validée (motif obligatoire)
 */
@RestController
@RequestMapping("/api/collecte/demandes")
@PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
@RequiredArgsConstructor
public class DemandesCollecteController {

    private final DemandesCollecteService demandesService;

    public record ValidationRequete(@Size(max = 300, message = "300 caractères au maximum.") String commentaire) {
    }

    public record RejetRequete(
            @NotBlank(message = "Indiquez le motif du rejet : il est envoyé à l'agriculteur.")
            @Size(max = 300, message = "300 caractères au maximum.")
            String motif) {
    }

    @GetMapping
    public PageResultat<DemandesCollecteService.DemandeCollecteReponse> lister(
            @RequestParam(required = false) StatutDemande statut,
            @RequestParam(required = false) TypeRecolte type,
            @RequestParam(required = false) String texte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return demandesService.lister(statut, type, texte, page, taille);
    }

    @GetMapping("/compteurs")
    public Map<String, Long> compteurs() {
        return demandesService.compteurs();
    }

    @GetMapping("/{id}")
    public DemandesCollecteService.FicheDemande fiche(@PathVariable String id) {
        return demandesService.fiche(id);
    }

    @PostMapping("/{id}/valider")
    public DemandesCollecteService.DemandeCollecteReponse valider(@PathVariable String id,
            @Valid @RequestBody ValidationRequete requete, @AuthenticationPrincipal Jwt auteur) {
        return demandesService.valider(id, requete.commentaire(), auteur.getSubject());
    }

    @PostMapping("/{id}/rejeter")
    public DemandesCollecteService.DemandeCollecteReponse rejeter(@PathVariable String id,
            @Valid @RequestBody RejetRequete requete, @AuthenticationPrincipal Jwt auteur) {
        return demandesService.rejeter(id, requete.motif(), auteur.getSubject());
    }
}
