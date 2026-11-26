package tn.oliveconnect.connexion;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Compte de la personne connectée (tous les rôles ; le jeton d'accès est obligatoire).
 * GET /api/profil                 mes informations
 * PUT /api/profil/mot-de-passe    changer mon mot de passe (obligatoire après un mot de passe provisoire)
 */
@RestController
@RequiredArgsConstructor
public class ProfilController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;

    @GetMapping("/api/profil")
    public ProfilReponse profil(@AuthenticationPrincipal Jwt jeton) {
        return ProfilReponse.depuis(compte(jeton));
    }

    /**
     * Le mot de passe actuel est vérifié ; le nouveau doit respecter la règle (8 caractères, une lettre, un chiffre),
     * être confirmé et différer de l'actuel. Il n'est plus « provisoire » ensuite.
     */
    @PutMapping("/api/profil/mot-de-passe")
    public ProfilReponse changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequete requete,
            @AuthenticationPrincipal Jwt jeton) {
        Utilisateur compte = compte(jeton);
        Map<String, String> erreurs = new LinkedHashMap<>();
        if (!encodeurMotDePasse.matches(requete.motDePasseActuel(), compte.getMotDePasse())) {
            erreurs.put("motDePasseActuel", "Le mot de passe actuel est incorrect.");
        }
        if (!requete.nouveauMotDePasse().equals(requete.confirmation())) {
            erreurs.put("confirmation", "Les deux mots de passe ne sont pas identiques.");
        }
        if (requete.nouveauMotDePasse().equals(requete.motDePasseActuel())) {
            erreurs.put("nouveauMotDePasse", "Le nouveau mot de passe doit être différent de l'actuel.");
        }
        if (!erreurs.isEmpty()) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.", erreurs);
        }
        compte.setMotDePasse(encodeurMotDePasse.encode(requete.nouveauMotDePasse()));
        compte.setMotDePasseProvisoire(false);
        return ProfilReponse.depuis(utilisateurRepository.save(compte));
    }

    private Utilisateur compte(Jwt jeton) {
        return utilisateurRepository.findById(jeton.getSubject())
                .orElseThrow(() -> new ErreurMetierException(HttpStatus.NOT_FOUND, "COMPTE_INTROUVABLE", "Ce compte n'existe plus."));
    }
}
