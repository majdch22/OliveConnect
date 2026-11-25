package tn.oliveconnect.connexion;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/** GET /api/profil : informations du compte connecté (le jeton d'accès est obligatoire). */
@RestController
@RequiredArgsConstructor
public class ProfilController {

    private final UtilisateurRepository utilisateurRepository;

    @GetMapping("/api/profil")
    public ProfilReponse profil(@AuthenticationPrincipal Jwt jeton) {
        return utilisateurRepository.findById(jeton.getSubject())
                .map(ProfilReponse::depuis)
                .orElseThrow(() -> new ErreurMetierException(HttpStatus.NOT_FOUND,
                        "COMPTE_INTROUVABLE", "Ce compte n'existe plus."));
    }
}
