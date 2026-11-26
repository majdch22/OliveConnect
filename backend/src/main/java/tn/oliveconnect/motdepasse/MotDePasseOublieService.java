package tn.oliveconnect.motdepasse;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.motdepasse.MotDePasseOublieRequetes.CodeDemande;
import tn.oliveconnect.motdepasse.MotDePasseOublieRequetes.NouveauMotDePasse;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.sms.MotifSms;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * « Mot de passe oublié » en trois étapes (décision 10) :
 * 1. la personne saisit son téléphone ou son e-mail ; un code à 6 chiffres part par SMS, valable 10 minutes ;
 * 2. elle saisit le code : 3 essais, ensuite le code est annulé et il faut en demander un nouveau ;
 * 3. elle choisit un nouveau mot de passe ; le blocage éventuel du compte est levé.
 * Protections :
 * - la réponse de l'étape 1 est identique que le compte existe ou non (rien à apprendre sur les inscrits) ;
 * - un nouveau code ne part qu'une minute après le précédent (pas d'envoi de SMS en rafale) ;
 * - seuls les comptes actifs reçoivent un code.
 */
@Service
@RequiredArgsConstructor
public class MotDePasseOublieService {

    static final Duration VALIDITE = Duration.ofMinutes(10);
    static final Duration DELAI_RENVOI = Duration.ofMinutes(1);
    static final int ESSAIS = 3;

    private final CodeReinitialisationRepository codeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder encodeur;
    private final Clock horloge;
    private final SecureRandom hasard = new SecureRandom();

    public CodeDemande demanderCode(String identifiant) {
        Instant maintenant = horloge.instant();
        utilisateurRepository.chercherParIdentifiant(identifiant)
                .filter(Utilisateur::estActif)
                .filter(compte -> peutRecevoirUnCode(compte, maintenant))
                .ifPresent(compte -> envoyerCode(compte, maintenant));
        return new CodeDemande("Si ce compte existe, un code vient de partir par SMS vers son numéro de téléphone.",
                (int) DELAI_RENVOI.toSeconds(), (int) VALIDITE.toMinutes());
    }

    /** Étape 2 : vérifie le code sans le consommer (il resservira à l'étape 3). */
    public void verifierCode(String identifiant, String code) {
        verifier(identifiant, code);
    }

    /** Étape 3 : vérifie le code une dernière fois, enregistre le nouveau mot de passe et supprime le code. */
    public void reinitialiser(NouveauMotDePasse requete) {
        if (!requete.nouveauMotDePasse().equals(requete.confirmation())) {
            String message = "Les deux mots de passe ne sont pas identiques.";
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES", message, Map.of("confirmation", message));
        }
        Utilisateur compte = verifier(requete.identifiant(), requete.code());
        compte.setMotDePasse(encodeur.encode(requete.nouveauMotDePasse()));
        compte.setMotDePasseProvisoire(false);
        // Le blocage après trop d'échecs est levé : la personne a prouvé qu'elle possède le téléphone du compte
        compte.setTentativesEchouees(0);
        compte.setBloqueJusqua(null);
        utilisateurRepository.save(compte);
        codeRepository.deleteByUtilisateurId(compte.getId());
    }

    // --- Règles ---

    private boolean peutRecevoirUnCode(Utilisateur compte, Instant maintenant) {
        return codeRepository.findByUtilisateurId(compte.getId())
                .map(precedent -> !precedent.getDateCreation().plus(DELAI_RENVOI).isAfter(maintenant))
                .orElse(true);
    }

    private void envoyerCode(Utilisateur compte, Instant maintenant) {
        String code = String.format("%06d", hasard.nextInt(1_000_000));
        codeRepository.deleteByUtilisateurId(compte.getId());
        CodeReinitialisation nouveau = new CodeReinitialisation();
        nouveau.setUtilisateurId(compte.getId());
        nouveau.setCodeChiffre(encodeur.encode(code));
        nouveau.setEssaisRestants(ESSAIS);
        nouveau.setDateCreation(maintenant);
        codeRepository.save(nouveau);
        notificationService.envoyerSmsSeul(compte, "OliveConnect : votre code est " + code
                + ". Il est valable 10 minutes. Ne le communiquez à personne.", MotifSms.CODE_MOT_DE_PASSE);
    }

    /**
     * Le code doit exister, ne pas avoir expiré et correspondre. Un code faux use un essai ; au dernier essai raté,
     * il est supprimé. Compte inconnu et code expiré donnent le même message.
     */
    private Utilisateur verifier(String identifiant, String code) {
        Optional<Utilisateur> compte = utilisateurRepository.chercherParIdentifiant(identifiant).filter(Utilisateur::estActif);
        CodeReinitialisation enregistre = compte.flatMap(trouve -> codeRepository.findByUtilisateurId(trouve.getId()))
                .filter(trouve -> trouve.getDateCreation().plus(VALIDITE).isAfter(horloge.instant()))
                .orElseThrow(MotDePasseOublieService::codeExpire);

        if (!encodeur.matches(code, enregistre.getCodeChiffre())) {
            enregistre.setEssaisRestants(enregistre.getEssaisRestants() - 1);
            if (enregistre.getEssaisRestants() <= 0) {
                codeRepository.delete(enregistre);
                throw codeExpire();
            }
            codeRepository.save(enregistre);
            String message = "Code incorrect. Il vous reste " + enregistre.getEssaisRestants()
                    + (enregistre.getEssaisRestants() > 1 ? " essais." : " essai.");
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "CODE_INCORRECT", message, Map.of("code", message));
        }
        return compte.get();
    }

    private static ErreurMetierException codeExpire() {
        return new ErreurMetierException(HttpStatus.BAD_REQUEST, "CODE_EXPIRE",
                "Ce code n'est plus valable (expiré, ou annulé après 3 essais). Demandez un nouveau code.");
    }
}
