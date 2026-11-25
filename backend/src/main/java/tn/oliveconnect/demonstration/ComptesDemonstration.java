package tn.oliveconnect.demonstration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Identifiants;
import tn.oliveconnect.utilisateur.InfosAgriculteur;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Comptes de démonstration, créés seulement en développement (profil « dev »).
 * Un compte par rôle, plus deux comptes pour montrer les refus de connexion.
 * Mot de passe commun : {@value #MOT_DE_PASSE}
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class ComptesDemonstration implements ApplicationRunner {

    public static final String MOT_DE_PASSE = "Olive2026!";

    private static final Logger journal = LoggerFactory.getLogger(ComptesDemonstration.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;

    @Override
    public void run(ApplicationArguments arguments) {
        creerSiAbsent("Ben Salah", "Ali", "ali.bensalah@oliveconnect.tn", "98123412", Role.AGRICULTEUR, StatutCompte.ACTIF);
        creerSiAbsent("Chaabane", "Salma", "salma.chaabane@oliveconnect.tn", "98200001", Role.RESPONSABLE_COLLECTE, StatutCompte.ACTIF);
        creerSiAbsent("Trabelsi", "Karim", "karim.trabelsi@oliveconnect.tn", "98200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        creerSiAbsent("Gharbi", "Rim", "rim.gharbi@oliveconnect.tn", "98200003", Role.RESPONSABLE_LOGISTIQUE, StatutCompte.ACTIF);
        creerSiAbsent("Ayadi", "Fethi", "fethi.ayadi@oliveconnect.tn", "98200004", Role.RESPONSABLE_PRESSOIR, StatutCompte.ACTIF);
        creerSiAbsent("Kammoun", "Nadia", "nadia.kammoun@oliveconnect.tn", "55000210", Role.AGRICULTEUR, StatutCompte.EN_ATTENTE_VALIDATION);
        creerSiAbsent("Jlassi", "Hamza", "hamza.jlassi@oliveconnect.tn", "55000211", Role.AGRICULTEUR, StatutCompte.DESACTIVE);

        ajouterInfosAgriculteur("ali.bensalah@oliveconnect.tn", "01234567", Delegation.SAKIET_EZZIT, "ADH-0042");
        ajouterInfosAgriculteur("nadia.kammoun@oliveconnect.tn", "09876543", Delegation.AGAREB, null);
        ajouterInfosAgriculteur("hamza.jlassi@oliveconnect.tn", "07654321", Delegation.EL_HENCHA, "ADH-0031");
    }

    /** Complète un compte agriculteur de démonstration avec sa carte d'identité et sa délégation. */
    private void ajouterInfosAgriculteur(String email, String cin, Delegation delegation, String numeroAdherent) {
        utilisateurRepository.findByEmail(email)
                .filter(agriculteur -> agriculteur.getAgriculteur() == null)
                .ifPresent(agriculteur -> {
                    InfosAgriculteur infos = new InfosAgriculteur();
                    infos.setCin(cin);
                    infos.setDelegation(delegation);
                    infos.setNumeroAdherent(numeroAdherent);
                    agriculteur.setAgriculteur(infos);
                    utilisateurRepository.save(agriculteur);
                });
    }

    private void creerSiAbsent(String nom, String prenom, String email, String telephone, Role role, StatutCompte statut) {
        if (utilisateurRepository.existsByEmail(email)) {
            return;
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setTelephone(Identifiants.normaliserTelephone(telephone));
        utilisateur.setMotDePasse(encodeurMotDePasse.encode(MOT_DE_PASSE));
        utilisateur.setRole(role);
        utilisateur.setStatut(statut);
        utilisateurRepository.save(utilisateur);
        journal.info("Compte de démonstration créé : {} ({}, {})", email, role.getLibelle(), statut);
    }
}
