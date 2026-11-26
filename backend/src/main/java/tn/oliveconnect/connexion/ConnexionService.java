package tn.oliveconnect.connexion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.security.JetonService;
import tn.oliveconnect.security.JetonService.JetonGenere;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Vérifie une tentative de connexion et remet un jeton d'accès.
 *
 * Ordre des contrôles :
 * 1. le compte existe ;
 * 2. il n'est pas bloqué ;
 * 3. le mot de passe est correct (sinon l'échec est compté) ;
 * 4. le compte se connecte depuis la bonne page (administrateur ou non) ;
 * 5. le compte est actif.
 * Les étapes 4 et 5 ne sont atteintes qu'avec le bon mot de passe : un inconnu n'apprend rien sur le compte.
 * Le nombre de tentatives et la durée du blocage viennent des paramètres modifiables par l'administrateur.
 */
@Service
@RequiredArgsConstructor
public class ConnexionService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;
    private final JetonService jetonService;
    private final ParametresService parametresService;
    private final Clock horloge;

    public ConnexionReponse connecter(ConnexionRequete requete, EspaceConnexion espace) {
        int tentativesAvantBlocage = parametresService.tentativesAvantBlocage();
        Instant maintenant = horloge.instant();

        Utilisateur utilisateur = utilisateurRepository.chercherParIdentifiant(requete.identifiant())
                .orElseThrow(() -> ConnexionRefuseeException.identifiantsIncorrects(0));

        if (utilisateur.estBloque(maintenant)) {
            throw ConnexionRefuseeException.compteBloque(minutesRestantes(utilisateur, maintenant));
        }

        if (!encodeurMotDePasse.matches(requete.motDePasse(), utilisateur.getMotDePasse())) {
            utilisateur.enregistrerEchecConnexion(tentativesAvantBlocage, parametresService.dureeBlocage(), maintenant);
            utilisateurRepository.save(utilisateur);
            if (utilisateur.estBloque(maintenant)) {
                throw ConnexionRefuseeException.compteBloque(minutesRestantes(utilisateur, maintenant));
            }
            int restantes = tentativesAvantBlocage - utilisateur.getTentativesEchouees();
            throw ConnexionRefuseeException.identifiantsIncorrects(restantes);
        }

        if (!espace.accepte(utilisateur.getRole())) {
            throw ConnexionRefuseeException.mauvaisEspace(espace);
        }

        verifierCompteActif(utilisateur.getStatut());

        utilisateur.enregistrerConnexionReussie(maintenant);
        utilisateurRepository.save(utilisateur);

        JetonGenere jeton = jetonService.generer(utilisateur);
        return new ConnexionReponse(
                utilisateur.getId(),
                jeton.valeur(),
                jeton.expiration(),
                utilisateur.getRole(),
                utilisateur.getRole().getLibelle(),
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                utilisateur.isMotDePasseProvisoire());
    }

    private void verifierCompteActif(StatutCompte statut) {
        switch (statut) {
            case ACTIF -> { }
            case EN_ATTENTE_VALIDATION -> throw ConnexionRefuseeException.compteEnAttenteValidation();
            case REFUSE -> throw ConnexionRefuseeException.inscriptionRefusee();
            case DESACTIVE -> throw ConnexionRefuseeException.compteDesactive();
        }
    }

    /** Minutes restantes avant déblocage, arrondies vers le haut (au moins 1). */
    private long minutesRestantes(Utilisateur utilisateur, Instant maintenant) {
        long secondes = Duration.between(maintenant, utilisateur.getBloqueJusqua()).toSeconds();
        return Math.max(1, (secondes + 59) / 60);
    }
}
