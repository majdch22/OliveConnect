package tn.oliveconnect.utilisateur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.configuration.OliveConnectProperties;

/**
 * Au démarrage, crée le premier administrateur s'il n'existe aucun administrateur.
 * Sans lui, personne ne pourrait se connecter pour créer les autres comptes.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class CreationPremierAdministrateur implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(CreationPremierAdministrateur.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;
    private final OliveConnectProperties proprietes;

    @Override
    public void run(ApplicationArguments arguments) {
        if (utilisateurRepository.existsByRole(Role.ADMINISTRATEUR)) {
            return;
        }
        OliveConnectProperties.PremierAdministrateur configuration = proprietes.premierAdministrateur();

        Utilisateur administrateur = new Utilisateur();
        administrateur.setNom(configuration.nom());
        administrateur.setPrenom(configuration.prenom());
        administrateur.setEmail(Identifiants.normaliserEmail(configuration.email()));
        administrateur.setTelephone(Identifiants.normaliserTelephone(configuration.telephone()));
        administrateur.setMotDePasse(encodeurMotDePasse.encode(configuration.motDePasse()));
        administrateur.setRole(Role.ADMINISTRATEUR);
        administrateur.setStatut(StatutCompte.ACTIF);
        utilisateurRepository.save(administrateur);

        journal.info("Premier administrateur créé : {}", administrateur.getEmail());
    }
}
