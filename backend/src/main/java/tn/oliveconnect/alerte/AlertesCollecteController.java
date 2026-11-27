package tn.oliveconnect.alerte;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;

/**
 * Signalements traités par le responsable de collecte (et l'administrateur, qui peut tout faire).
 * GET  /api/collecte/alertes                          file filtrée (état, type), les plus récents d'abord
 * GET  /api/collecte/alertes/compteurs                nombre de signalements par état
 * GET  /api/collecte/alertes/{id}                     fiche et traitements
 * GET  /api/collecte/alertes/{id}/photo               photo jointe par l'agriculteur
 * POST /api/collecte/alertes/{id}/prendre-en-charge   commentaire facultatif
 * POST /api/collecte/alertes/{id}/resoudre            commentaire obligatoire (réponse à l'agriculteur)
 * POST /api/collecte/alertes/{id}/demande-urgente     créer une demande urgente pour le verger du signalement
 */
@RestController
@RequestMapping("/api/collecte/alertes")
@PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
@RequiredArgsConstructor
public class AlertesCollecteController {

    private final AlertesCollecteService alertesService;
    private final PhotoSignalementService photoService;

    public record PriseEnChargeRequete(@Size(max = 300, message = "300 caractères au maximum.") String commentaire) {
    }

    public record ResolutionRequete(
            @NotBlank(message = "Indiquez ce qui a été fait : l'agriculteur lira cette réponse.")
            @Size(max = 300, message = "300 caractères au maximum.")
            String commentaire) {
    }

    public record DemandeUrgenteRequete(
            @NotNull(message = "Choisissez la date de la récolte.")
            LocalDate date,
            @Min(value = 1, message = "Entre 1 et 100 000 kg.")
            @Max(value = 100000, message = "Entre 1 et 100 000 kg.")
            int quantiteEstimeeKg,
            @Size(max = 200, message = "200 caractères au maximum.")
            String commentaire) {
    }

    @GetMapping
    public PageResultat<AlertesCollecteService.AlerteCollecteReponse> lister(
            @RequestParam(required = false) StatutAlerte statut,
            @RequestParam(required = false) TypeAlerte type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return alertesService.lister(statut, type, page, taille);
    }

    @GetMapping("/compteurs")
    public Map<String, Long> compteurs() {
        return alertesService.compteurs();
    }

    @GetMapping("/{id}")
    public AlertesCollecteService.AlerteCollecteReponse consulter(@PathVariable String id) {
        return alertesService.consulter(id);
    }

    /** La photo n'est jamais mise en cache par un intermédiaire : elle reste privée. */
    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable String id) {
        PhotoSignalementService.Photo photo = photoService.lirePourLaCooperative(id);
        return ResponseEntity.ok().contentType(photo.type()).cacheControl(CacheControl.noStore()).body(photo.fichier());
    }

    @PostMapping("/{id}/prendre-en-charge")
    public AlertesCollecteService.AlerteCollecteReponse prendreEnCharge(@PathVariable String id,
            @Valid @RequestBody PriseEnChargeRequete requete, @AuthenticationPrincipal Jwt auteur) {
        return alertesService.prendreEnCharge(id, requete.commentaire(), auteur.getSubject());
    }

    @PostMapping("/{id}/resoudre")
    public AlertesCollecteService.AlerteCollecteReponse resoudre(@PathVariable String id,
            @Valid @RequestBody ResolutionRequete requete, @AuthenticationPrincipal Jwt auteur) {
        return alertesService.resoudre(id, requete.commentaire(), auteur.getSubject());
    }

    @PostMapping("/{id}/demande-urgente")
    public AlertesCollecteService.AlerteCollecteReponse demandeUrgente(@PathVariable String id,
            @Valid @RequestBody DemandeUrgenteRequete requete, @AuthenticationPrincipal Jwt auteur) {
        return alertesService.creerDemandeUrgente(id, requete.date(), requete.quantiteEstimeeKg(), requete.commentaire(),
                auteur.getSubject());
    }
}
