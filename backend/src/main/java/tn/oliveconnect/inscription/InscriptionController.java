package tn.oliveconnect.inscription;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.utilisateur.Delegation;

/**
 * Inscription des agriculteurs (accessible sans être connecté).
 * POST /api/inscription              : envoie le formulaire, crée le compte en attente de validation
 * GET  /api/inscription/delegations  : liste des délégations proposées dans le formulaire
 */
@RestController
@RequestMapping("/api/inscription")
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscriptionReponse inscrire(@Valid @RequestBody InscriptionRequete requete) {
        return inscriptionService.inscrire(requete);
    }

    @GetMapping("/delegations")
    public List<DelegationReponse> delegations() {
        return Arrays.stream(Delegation.values()).map(DelegationReponse::depuis).toList();
    }
}
