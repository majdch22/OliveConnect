package tn.oliveconnect.connexion;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Les deux adresses de connexion (accessibles sans être connecté).
 * POST /api/connexion                  : agriculteurs et personnel
 * POST /api/administration/connexion   : administrateurs
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConnexionController {

    private final ConnexionService connexionService;

    @PostMapping("/connexion")
    public ConnexionReponse connecter(@Valid @RequestBody ConnexionRequete requete) {
        return connexionService.connecter(requete, EspaceConnexion.UTILISATEURS);
    }

    @PostMapping("/administration/connexion")
    public ConnexionReponse connecterAdministrateur(@Valid @RequestBody ConnexionRequete requete) {
        return connexionService.connecter(requete, EspaceConnexion.ADMINISTRATION);
    }
}
