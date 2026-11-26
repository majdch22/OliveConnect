package tn.oliveconnect.parametres;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Paramètres de la plateforme (réservés à l'administrateur).
 * GET /api/administration/parametres              valeurs actuelles + fréquences possibles
 * PUT /api/administration/parametres              enregistre, applique immédiatement
 */
@RestController
@RequestMapping("/api/administration/parametres")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class ParametresController {

    private final ParametresService parametresService;
    private final UtilisateurRepository utilisateurRepository;

    /** Paramètres et liste des fréquences proposées dans les menus déroulants. */
    public record ParametresReponse(Parametres parametres, List<Frequence> frequencesPossibles) {
        public record Frequence(FrequenceImport code, String libelle) {
        }
    }

    @GetMapping
    public ParametresReponse consulter() {
        return reponse(parametresService.actuels());
    }

    @PutMapping
    public ParametresReponse modifier(@Valid @RequestBody ParametresRequete requete,
            @AuthenticationPrincipal Jwt administrateur) {
        String auteur = utilisateurRepository.findById(administrateur.getSubject())
                .map(compte -> compte.getNomComplet())
                .orElse("Administrateur");
        return reponse(parametresService.modifier(requete, auteur));
    }

    private static ParametresReponse reponse(Parametres parametres) {
        List<ParametresReponse.Frequence> frequences = Arrays.stream(FrequenceImport.values())
                .map(frequence -> new ParametresReponse.Frequence(frequence, frequence.getLibelle()))
                .toList();
        return new ParametresReponse(parametres, frequences);
    }
}
