package tn.oliveconnect.inscription;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.utilisateur.Identifiants;
import tn.oliveconnect.utilisateur.InfosAgriculteur;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Inscription d'un agriculteur depuis le site public.
 * Le compte est créé « en attente de validation » : il ne peut pas se connecter
 * tant que l'administrateur n'a pas vérifié l'inscription.
 */
@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;
    private final Clock horloge;

    public InscriptionReponse inscrire(InscriptionRequete requete) {
        String telephone = Identifiants.normaliserTelephone(requete.telephone());
        String email = requete.email() == null || requete.email().isBlank()
                ? null
                : Identifiants.normaliserEmail(requete.email());

        verifierSaisie(requete, telephone);
        verifierDisponibilite(telephone, email, requete.cin());

        InfosAgriculteur infos = new InfosAgriculteur();
        infos.setCin(requete.cin());
        infos.setDelegation(requete.delegation());
        infos.setDateAcceptationConditions(horloge.instant());

        Utilisateur agriculteur = new Utilisateur();
        agriculteur.setNom(requete.nom().trim());
        agriculteur.setPrenom(requete.prenom().trim());
        agriculteur.setTelephone(telephone);
        agriculteur.setEmail(email);
        agriculteur.setMotDePasse(encodeurMotDePasse.encode(requete.motDePasse()));
        agriculteur.setRole(Role.AGRICULTEUR);
        agriculteur.setStatut(StatutCompte.EN_ATTENTE_VALIDATION);
        agriculteur.setAgriculteur(infos);

        try {
            utilisateurRepository.save(agriculteur);
        } catch (DuplicateKeyException doublon) {
            // Deux inscriptions identiques envoyées au même instant : l'index unique de la base tranche.
            throw new ErreurMetierException(HttpStatus.CONFLICT, "DONNEES_DEJA_UTILISEES",
                    "Un compte existe déjà avec ce téléphone, cet e-mail ou cette carte d'identité.");
        }
        return new InscriptionReponse(agriculteur.getPrenom(), masquer(telephone));
    }

    /** Contrôles qui dépendent de plusieurs champs ou d'une mise en forme. */
    private void verifierSaisie(InscriptionRequete requete, String telephone) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        if (!telephone.matches("\\+216\\d{8}")) {
            erreurs.put("telephone", "Saisissez un numéro tunisien à 8 chiffres.");
        }
        if (!requete.motDePasse().equals(requete.confirmationMotDePasse())) {
            erreurs.put("confirmationMotDePasse", "Les deux mots de passe ne sont pas identiques.");
        }
        if (!erreurs.isEmpty()) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.", erreurs);
        }
    }

    /** Téléphone, e-mail et carte d'identité ne peuvent appartenir qu'à un seul compte. */
    private void verifierDisponibilite(String telephone, String email, String cin) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        if (utilisateurRepository.existsByTelephone(telephone)) {
            erreurs.put("telephone", "Ce numéro est déjà utilisé par un autre compte.");
        }
        if (email != null && utilisateurRepository.existsByEmail(email)) {
            erreurs.put("email", "Cette adresse e-mail est déjà utilisée par un autre compte.");
        }
        if (utilisateurRepository.existsByAgriculteurCin(cin)) {
            erreurs.put("cin", "Cette carte d'identité est déjà enregistrée.");
        }
        if (!erreurs.isEmpty()) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "DONNEES_DEJA_UTILISEES",
                    "Un compte existe déjà avec certaines de ces informations.", erreurs);
        }
    }

    /** +21698123412 devient « +216 98 ••• 412 ». */
    static String masquer(String telephone) {
        return "+216 " + telephone.substring(4, 6) + " ••• " + telephone.substring(9);
    }
}
