package tn.oliveconnect.pressoir;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.referentiel.PressoirReponse;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Espace du responsable du pressoir : il n'agit que sur son pressoir. L'administrateur, qui peut tout faire,
 * ajoute ?pressoirId=… à chaque requête.
 * GET  /api/pressoir                              mon pressoir et sa capacité aujourd'hui et demain
 * GET  /api/pressoir/livraisons?date=             tournées attendues ce jour-là et leurs bennes
 * POST /api/pressoir/receptions                   réceptionner une benne (poids, acidité)
 * GET  /api/pressoir/receptions?du=&au=&page=     historique des réceptions
 * GET  /api/pressoir/receptions/export?du=&au=    les mêmes réceptions en fichier CSV (tableur)
 * GET  /api/pressoir/capacites?du=&au=            capacité jour par jour (maximum, attendu, reçu, restant)
 * PUT  /api/pressoir/capacites/{date}             publier la capacité d'un jour (aujourd'hui ou à venir)
 * GET  /api/pressoir/tableau-de-bord?jours=       chiffres des 7 ou 30 derniers jours
 */
@RestController
@RequestMapping("/api/pressoir")
@PreAuthorize("hasAnyRole('RESPONSABLE_PRESSOIR', 'ADMINISTRATEUR')")
@RequiredArgsConstructor
public class EspacePressoirController {

    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Africa/Tunis"));

    private final PressoirDuCompte pressoirDuCompte;
    private final ReceptionService receptionService;
    private final CapaciteService capaciteService;
    private final TableauDeBordPressoirService tableauDeBordService;
    private final UtilisateurRepository utilisateurRepository;
    private final Clock horloge;

    public record MonPressoir(PressoirReponse pressoir, CapaciteService.CapaciteDuJour aujourdhui,
            CapaciteService.CapaciteDuJour demain) {
    }

    public record CapaciteRequete(
            @Min(value = 0, message = "Entre 0 et 1 000 000 kg.")
            @Max(value = 1_000_000, message = "Entre 0 et 1 000 000 kg.")
            int maximumKg,
            @Size(max = 200, message = "200 caractères au maximum.")
            String motif) {
    }

    @GetMapping
    public MonPressoir monPressoir(@AuthenticationPrincipal Jwt compte, @RequestParam(required = false) String pressoirId) {
        var contexte = pressoirDuCompte.resoudre(compte.getSubject(), pressoirId);
        LocalDate aujourdhui = LocalDate.now(horloge);
        return new MonPressoir(PressoirReponse.depuis(contexte.pressoir(), utilisateurRepository.countByPressoirId(contexte.pressoir().getId())),
                capaciteService.duJour(contexte.pressoir(), aujourdhui), capaciteService.duJour(contexte.pressoir(), aujourdhui.plusDays(1)));
    }

    @GetMapping("/livraisons")
    public List<ReceptionService.TourneeAttendue> livraisons(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return receptionService.attendues(pressoirDuCompte.resoudre(compte.getSubject(), pressoirId).pressoir(),
                date == null ? LocalDate.now(horloge) : date);
    }

    @PostMapping("/receptions")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceptionService.ReceptionReponse receptionner(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId, @Valid @RequestBody ReceptionRequete requete) {
        var contexte = pressoirDuCompte.resoudre(compte.getSubject(), pressoirId);
        return receptionService.receptionner(requete, contexte.pressoir(), contexte.compte());
    }

    @GetMapping("/receptions")
    public PageResultat<ReceptionService.ReceptionReponse> historique(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        LocalDate fin = au == null ? LocalDate.now(horloge) : au;
        return receptionService.historique(pressoirDuCompte.resoudre(compte.getSubject(), pressoirId).pressoir(),
                du == null ? fin : du, fin, page, taille);
    }

    /** Fichier CSV séparé par des points-virgules (ouvert directement par un tableur réglé en français). */
    @GetMapping("/receptions/export")
    public ResponseEntity<byte[]> exporter(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        var pressoir = pressoirDuCompte.resoudre(compte.getSubject(), pressoirId).pressoir();
        StringBuilder csv = new StringBuilder("\uFEFFNuméro;Date et heure;Tournée;Benne;Capacité (kg);Poids net (kg);Remplissage (%);Acidité (%);Catégorie;Reçue par\n");
        for (Reception reception : receptionService.toutes(pressoir, du, au)) {
            csv.append(String.join(";", reception.getNumero(), FORMAT_HEURE.format(reception.getDateHeure()),
                    reception.getNumeroTournee(), reception.getCodeBenne(), String.valueOf(reception.getCapaciteBenneKg()),
                    String.valueOf(reception.getPoidsNetKg()), String.valueOf(reception.getTauxRemplissage()),
                    String.valueOf(reception.getAciditePourcent()).replace('.', ','), reception.getCategorie().getLibelle(),
                    cellule(reception.getNomAuteur()))).append('\n');
        }
        String nomFichier = "receptions-" + pressoir.getCode() + "-" + du + "-" + au + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/capacites")
    public List<CapaciteService.CapaciteDuJour> capacites(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        LocalDate fin = au.isAfter(du.plusDays(62)) ? du.plusDays(62) : au;
        return capaciteService.periode(pressoirDuCompte.resoudre(compte.getSubject(), pressoirId).pressoir(), du, fin);
    }

    @PutMapping("/capacites/{date}")
    public CapaciteService.CapaciteDuJour publier(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CapaciteRequete requete) {
        var contexte = pressoirDuCompte.resoudre(compte.getSubject(), pressoirId);
        return capaciteService.publier(contexte.pressoir(), date, requete.maximumKg(), requete.motif(), contexte.compte());
    }

    @GetMapping("/tableau-de-bord")
    public TableauDeBordPressoirService.TableauDeBord tableauDeBord(@AuthenticationPrincipal Jwt compte,
            @RequestParam(required = false) String pressoirId, @RequestParam(defaultValue = "7") int jours) {
        return tableauDeBordService.donnees(pressoirDuCompte.resoudre(compte.getSubject(), pressoirId).pressoir(), jours);
    }

    /** Un nom ne doit pas couper la ligne du fichier CSV. */
    private static String cellule(String texte) {
        return texte == null ? "" : texte.replace(';', ',').replace('\n', ' ');
    }
}
