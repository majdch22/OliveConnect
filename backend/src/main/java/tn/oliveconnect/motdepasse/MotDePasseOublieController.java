package tn.oliveconnect.motdepasse;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.motdepasse.MotDePasseOublieRequetes.CodeDemande;
import tn.oliveconnect.motdepasse.MotDePasseOublieRequetes.DemandeCode;
import tn.oliveconnect.motdepasse.MotDePasseOublieRequetes.NouveauMotDePasse;
import tn.oliveconnect.motdepasse.MotDePasseOublieRequetes.VerificationCode;

/**
 * « Mot de passe oublié » (public, sans jeton d'accès).
 * POST /api/mot-de-passe-oublie/code           étape 1 : recevoir un code par SMS
 * POST /api/mot-de-passe-oublie/verification   étape 2 : vérifier le code
 * POST /api/mot-de-passe-oublie/nouveau        étape 3 : choisir le nouveau mot de passe
 */
@RestController
@RequestMapping("/api/mot-de-passe-oublie")
@RequiredArgsConstructor
public class MotDePasseOublieController {

    private final MotDePasseOublieService motDePasseOublieService;

    @PostMapping("/code")
    public CodeDemande demanderCode(@Valid @RequestBody DemandeCode requete) {
        return motDePasseOublieService.demanderCode(requete.identifiant());
    }

    @PostMapping("/verification")
    public Map<String, Boolean> verifierCode(@Valid @RequestBody VerificationCode requete) {
        motDePasseOublieService.verifierCode(requete.identifiant(), requete.code());
        return Map.of("valide", true);
    }

    @PostMapping("/nouveau")
    public Map<String, Boolean> reinitialiser(@Valid @RequestBody NouveauMotDePasse requete) {
        motDePasseOublieService.reinitialiser(requete);
        return Map.of("reinitialise", true);
    }
}
