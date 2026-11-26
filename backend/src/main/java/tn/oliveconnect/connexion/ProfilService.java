package tn.oliveconnect.connexion;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.utilisateur.Identifiants;
import tn.oliveconnect.utilisateur.PreferencesNotification;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.utilisateur.VerificationIdentifiants;

/**
 * « Mon profil » de la personne connectée (tous les rôles).
 * Règles : le mot de passe actuel est redemandé pour changer le mot de passe ou les coordonnées ;
 * le téléphone et l'e-mail ne doivent appartenir à aucun autre compte.
 */
@Service
@RequiredArgsConstructor
public class ProfilService {

    private final UtilisateurRepository utilisateurRepository;
    private final VerificationIdentifiants verificationIdentifiants;
    private final PasswordEncoder encodeurMotDePasse;

    public ProfilReponse profil(String compteId) {
        return ProfilReponse.depuis(compte(compteId));
    }

    /**
     * Le nouveau mot de passe doit respecter la règle (8 caractères, une lettre, un chiffre),
     * être confirmé et différer de l'actuel. Il n'est plus « provisoire » ensuite.
     */
    public ProfilReponse changerMotDePasse(String compteId, ChangementMotDePasseRequete requete) {
        Utilisateur compte = compte(compteId);
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
        verifierAucuneErreur(erreurs);
        compte.setMotDePasse(encodeurMotDePasse.encode(requete.nouveauMotDePasse()));
        compte.setMotDePasseProvisoire(false);
        return ProfilReponse.depuis(utilisateurRepository.save(compte));
    }

    /**
     * Le téléphone doit rester un numéro tunisien, et ni lui ni l'e-mail ne doivent appartenir à un autre compte (409).
     * La délégation n'existe que pour un agriculteur ; elle est ignorée pour le personnel.
     */
    public ProfilReponse changerCoordonnees(String compteId, CoordonneesRequete requete) {
        Utilisateur compte = compte(compteId);
        String telephone = Identifiants.normaliserTelephone(requete.telephone());
        String email = requete.email() == null || requete.email().isBlank() ? null : Identifiants.normaliserEmail(requete.email());

        Map<String, String> erreurs = new LinkedHashMap<>();
        if (!encodeurMotDePasse.matches(requete.motDePasse(), compte.getMotDePasse())) {
            erreurs.put("motDePasse", "Le mot de passe est incorrect.");
        }
        if (!VerificationIdentifiants.estTelephoneTunisien(telephone)) {
            erreurs.put("telephone", "Saisissez un numéro tunisien à 8 chiffres.");
        }
        if (compte.getAgriculteur() != null && requete.delegation() == null) {
            erreurs.put("delegation", "Choisissez votre délégation.");
        }
        verifierAucuneErreur(erreurs);
        verificationIdentifiants.verifierDisponibles(telephone, email, null, compte.getId());

        compte.setTelephone(telephone);
        compte.setEmail(email);
        if (compte.getAgriculteur() != null) {
            compte.getAgriculteur().setDelegation(requete.delegation());
        }
        return ProfilReponse.depuis(utilisateurRepository.save(compte));
    }

    public ProfilReponse changerPreferences(String compteId, PreferencesNotification preferences) {
        Utilisateur compte = compte(compteId);
        compte.setPreferencesNotification(preferences);
        return ProfilReponse.depuis(utilisateurRepository.save(compte));
    }

    public ProfilReponse terminerVisiteGuidee(String compteId) {
        Utilisateur compte = compte(compteId);
        compte.setVisiteGuideeTerminee(true);
        return ProfilReponse.depuis(utilisateurRepository.save(compte));
    }

    private Utilisateur compte(String compteId) {
        return utilisateurRepository.findById(compteId)
                .orElseThrow(() -> new ErreurMetierException(HttpStatus.NOT_FOUND, "COMPTE_INTROUVABLE", "Ce compte n'existe plus."));
    }

    private static void verifierAucuneErreur(Map<String, String> erreurs) {
        if (!erreurs.isEmpty()) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.", erreurs);
        }
    }
}
