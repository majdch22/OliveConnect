package tn.oliveconnect.alerte;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Signalements de l'agriculteur connecté.
 * GET  /api/agriculteur/signalements    mes signalements, des plus récents aux plus anciens
 * POST /api/agriculteur/signalements    signaler maturité ou problème sur un de mes vergers
 * POST /api/agriculteur/signalements/{id}/photo   joindre une photo (formulaire multipart, champ « photo »)
 * GET  /api/agriculteur/signalements/{id}/photo   voir la photo jointe
 */
@RestController
@RequestMapping("/api/agriculteur/signalements")
@PreAuthorize("hasRole('AGRICULTEUR')")
@RequiredArgsConstructor
public class SignalementController {

    private final AlerteService alerteService;
    private final PhotoSignalementService photoService;

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

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AlerteService.AlerteReponse joindrePhoto(@PathVariable String id, @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal Jwt agriculteur) {
        return photoService.enregistrer(id, photo, agriculteur.getSubject());
    }

    /** La photo n'est jamais mise en cache par un intermédiaire : elle reste privée. */
    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> voirPhoto(@PathVariable String id, @AuthenticationPrincipal Jwt agriculteur) {
        PhotoSignalementService.Photo photo = photoService.lire(id, agriculteur.getSubject());
        return ResponseEntity.ok().contentType(photo.type()).cacheControl(CacheControl.noStore()).body(photo.fichier());
    }
}
