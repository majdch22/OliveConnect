package tn.oliveconnect.connexion;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.utilisateur.PreferencesNotification;

/**
 * Compte de la personne connectée (tous les rôles ; le jeton d'accès est obligatoire).
 * La personne est toujours celle du jeton : on ne peut agir que sur son propre compte. Les règles sont dans {@link ProfilService}.
 * GET  /api/profil                   mes informations
 * PUT  /api/profil/mot-de-passe      changer mon mot de passe (obligatoire après un mot de passe provisoire)
 * PUT  /api/profil/coordonnees       changer mon téléphone, mon e-mail, ma délégation (mot de passe redemandé)
 * PUT  /api/profil/preferences       choisir comment être prévenu
 * POST /api/profil/visite-guidee     la visite guidée est terminée ou passée : ne plus la proposer
 */
@RestController
@RequiredArgsConstructor
public class ProfilController {

    private final ProfilService profilService;

    @GetMapping("/api/profil")
    public ProfilReponse profil(@AuthenticationPrincipal Jwt jeton) {
        return profilService.profil(jeton.getSubject());
    }

    @PutMapping("/api/profil/mot-de-passe")
    public ProfilReponse changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequete requete, @AuthenticationPrincipal Jwt jeton) {
        return profilService.changerMotDePasse(jeton.getSubject(), requete);
    }

    @PutMapping("/api/profil/coordonnees")
    public ProfilReponse changerCoordonnees(@Valid @RequestBody CoordonneesRequete requete, @AuthenticationPrincipal Jwt jeton) {
        return profilService.changerCoordonnees(jeton.getSubject(), requete);
    }

    @PutMapping("/api/profil/preferences")
    public ProfilReponse changerPreferences(@RequestBody PreferencesNotification preferences, @AuthenticationPrincipal Jwt jeton) {
        return profilService.changerPreferences(jeton.getSubject(), preferences);
    }

    @PostMapping("/api/profil/visite-guidee")
    public ProfilReponse terminerVisiteGuidee(@AuthenticationPrincipal Jwt jeton) {
        return profilService.terminerVisiteGuidee(jeton.getSubject());
    }
}
