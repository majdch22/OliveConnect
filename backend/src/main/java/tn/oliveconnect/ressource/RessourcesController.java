package tn.oliveconnect.ressource;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;

/**
 * Ressources et équipes, gérées par le responsable logistique (et l'administrateur, qui peut tout faire).
 * GET  /api/ressources                       liste filtrée (texte, type, état, équipe, archives), triée et paginée
 * GET  /api/ressources/resume                total et disponibles par type
 * POST /api/ressources                       ajout          PUT /api/ressources/{id}   modification
 * POST /api/ressources/{id}/archiver         archivage      POST /api/ressources/{id}/restaurer
 * GET  /api/equipes                          équipes et effectifs
 * GET  /api/equipes/{id}/membres             ouvriers d'une équipe
 * POST /api/equipes  ·  PUT /api/equipes/{id}  ·  POST /api/equipes/{id}/archiver|restaurer
 */
@RestController
@PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'RESPONSABLE_LOGISTIQUE')")
@RequiredArgsConstructor
public class RessourcesController {

    private final RessourceService ressourceService;
    private final EquipeService equipeService;

    // --- Ressources ---

    @GetMapping("/api/ressources")
    public PageResultat<RessourceReponse> lister(
            @RequestParam(required = false) String texte,
            @RequestParam(required = false) TypeRessource type,
            @RequestParam(required = false) EtatRessource etat,
            @RequestParam(required = false) String equipeId,
            @RequestParam(defaultValue = "false") boolean avecArchives,
            @RequestParam(defaultValue = "CODE") RessourceService.Tri tri,
            @RequestParam(defaultValue = "ASC") Sort.Direction sens,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return ressourceService.lister(texte, type, etat, equipeId, avecArchives, tri, sens, page, taille);
    }

    @GetMapping("/api/ressources/resume")
    public List<RessourceService.ResumeParType> resume() {
        return ressourceService.resume();
    }

    @PostMapping("/api/ressources")
    @ResponseStatus(HttpStatus.CREATED)
    public RessourceReponse creer(@Valid @RequestBody RessourceRequete requete) {
        return ressourceService.creer(requete);
    }

    @PutMapping("/api/ressources/{id}")
    public RessourceReponse modifier(@PathVariable String id, @Valid @RequestBody RessourceRequete requete) {
        return ressourceService.modifier(id, requete);
    }

    @PostMapping("/api/ressources/{id}/archiver")
    public RessourceReponse archiver(@PathVariable String id) {
        return ressourceService.changerArchivage(id, true);
    }

    @PostMapping("/api/ressources/{id}/restaurer")
    public RessourceReponse restaurer(@PathVariable String id) {
        return ressourceService.changerArchivage(id, false);
    }

    // --- Équipes ---

    @GetMapping("/api/equipes")
    public List<EquipeService.EquipeReponse> equipes(@RequestParam(defaultValue = "false") boolean avecArchives) {
        return equipeService.lister(avecArchives);
    }

    @GetMapping("/api/equipes/{id}/membres")
    public List<RessourceReponse> membres(@PathVariable String id) {
        return equipeService.membres(id);
    }

    @PostMapping("/api/equipes")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipeService.EquipeReponse creerEquipe(@Valid @RequestBody EquipeService.EquipeRequete requete) {
        return equipeService.creer(requete);
    }

    @PutMapping("/api/equipes/{id}")
    public EquipeService.EquipeReponse renommerEquipe(@PathVariable String id,
            @Valid @RequestBody EquipeService.EquipeRequete requete) {
        return equipeService.renommer(id, requete);
    }

    @PostMapping("/api/equipes/{id}/archiver")
    public EquipeService.EquipeReponse archiverEquipe(@PathVariable String id) {
        return equipeService.changerArchivage(id, true);
    }

    @PostMapping("/api/equipes/{id}/restaurer")
    public EquipeService.EquipeReponse restaurerEquipe(@PathVariable String id) {
        return equipeService.changerArchivage(id, false);
    }
}
