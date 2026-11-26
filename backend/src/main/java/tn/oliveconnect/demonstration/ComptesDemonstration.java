package tn.oliveconnect.demonstration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
 * Comptes de démonstration, créés seulement en développement (profil « dev ») et seulement s'ils n'existent pas.
 * Un membre du personnel par rôle, et des agriculteurs dans plusieurs délégations et plusieurs états,
 * pour que les listes de l'administration aient de quoi être filtrées, triées et paginées.
 * Mot de passe commun : {@value #MOT_DE_PASSE}
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class ComptesDemonstration implements ApplicationRunner {

    public static final String MOT_DE_PASSE = "Olive2026!";

    private static final Logger journal = LoggerFactory.getLogger(ComptesDemonstration.class);

    /** Un compte du personnel. */
    private record Personnel(String prenom, String nom, String email, String telephone, Role role) {
    }

    /** Un agriculteur. L'ancienneté (en jours) étale les dates d'inscription pour le tri. */
    private record Agriculteur(String prenom, String nom, String telephone, String cin, Delegation delegation,
            StatutCompte statut, String numeroAdherent, int ancienneteJours, String motifRefus) {
    }

    private static final List<Personnel> PERSONNEL = List.of(
            new Personnel("Salma", "Chaabane", "salma.chaabane@oliveconnect.tn", "98200001", Role.RESPONSABLE_COLLECTE),
            new Personnel("Karim", "Trabelsi", "karim.trabelsi@oliveconnect.tn", "98200002", Role.CHEF_EQUIPE),
            new Personnel("Sami", "Bouzid", "sami.bouzid@oliveconnect.tn", "98200005", Role.CHEF_EQUIPE),
            new Personnel("Rim", "Gharbi", "rim.gharbi@oliveconnect.tn", "98200003", Role.RESPONSABLE_LOGISTIQUE),
            new Personnel("Fethi", "Ayadi", "fethi.ayadi@oliveconnect.tn", "98200004", Role.RESPONSABLE_PRESSOIR));

    private static final List<Agriculteur> AGRICULTEURS = List.of(
            new Agriculteur("Ali", "Ben Salah", "98123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF, "ADH-0042", 60, null),
            new Agriculteur("Mounir", "Karray", "97111222", "02345678", Delegation.EL_HENCHA, StatutCompte.ACTIF, "ADH-0043", 55, null),
            new Agriculteur("Hela", "Frikha", "97111333", "03456789", Delegation.AGAREB, StatutCompte.ACTIF, "ADH-0044", 41, null),
            new Agriculteur("Youssef", "Ellouze", "97111444", "04567890", Delegation.THYNA, StatutCompte.ACTIF, "ADH-0045", 33, null),
            new Agriculteur("Amel", "Zouari", "97111555", "05678901", Delegation.SAKIET_EDDAIER, StatutCompte.ACTIF, "ADH-0046", 21, null),
            new Agriculteur("Hamza", "Jlassi", "55000211", "07654321", Delegation.EL_HENCHA, StatutCompte.DESACTIVE, "ADH-0031", 90, null),
            new Agriculteur("Nadia", "Kammoun", "55000210", "09876543", Delegation.AGAREB, StatutCompte.EN_ATTENTE_VALIDATION, null, 1, null),
            new Agriculteur("Omar", "Fourati", "55000212", "11223355", Delegation.EL_HENCHA, StatutCompte.EN_ATTENTE_VALIDATION, null, 2, null),
            new Agriculteur("Leila", "Masmoudi", "55000213", "11223366", Delegation.SAKIET_EDDAIER, StatutCompte.EN_ATTENTE_VALIDATION, null, 3, null),
            new Agriculteur("Riadh", "Baklouti", "55000214", "11223377", Delegation.JEBINIANA, StatutCompte.EN_ATTENTE_VALIDATION, null, 4, null),
            new Agriculteur("Sonia", "Abid", "55000215", "11223388", Delegation.MAHRES, StatutCompte.EN_ATTENTE_VALIDATION, null, 5, null),
            new Agriculteur("Walid", "Hammami", "55000216", "11223399", Delegation.AGAREB, StatutCompte.EN_ATTENTE_VALIDATION, null, 6, null),
            new Agriculteur("Ines", "Sellami", "55000217", "11224400", Delegation.SKHIRA, StatutCompte.EN_ATTENTE_VALIDATION, null, 8, null),
            new Agriculteur("Tarek", "Mejdoub", "55000218", "11224411", Delegation.KERKENNAH, StatutCompte.REFUSE, null, 12,
                    "Délégation hors de la zone de collecte de la coopérative."),
            new Agriculteur("Rania", "Gargouri", "55000219", "11224422", Delegation.SFAX_SUD, StatutCompte.REFUSE, null, 15,
                    "Informations incomplètes : carte d'identité illisible."));

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;
    private final Clock horloge;

    @Override
    public void run(ApplicationArguments arguments) {
        String motDePasseChiffre = encodeurMotDePasse.encode(MOT_DE_PASSE);
        PERSONNEL.forEach(personne -> creerPersonnel(personne, motDePasseChiffre));
        AGRICULTEURS.forEach(agriculteur -> creerAgriculteur(agriculteur, motDePasseChiffre));
    }

    private void creerPersonnel(Personnel personne, String motDePasseChiffre) {
        if (utilisateurRepository.existsByEmail(personne.email())) {
            return;
        }
        Utilisateur compte = compte(personne.prenom(), personne.nom(), personne.telephone(), personne.role(),
                StatutCompte.ACTIF, motDePasseChiffre);
        compte.setEmail(personne.email());
        utilisateurRepository.save(compte);
        journal.info("Compte de démonstration créé : {} ({})", personne.email(), personne.role().getLibelle());
    }

    private void creerAgriculteur(Agriculteur agriculteur, String motDePasseChiffre) {
        String telephone = Identifiants.normaliserTelephone(agriculteur.telephone());
        if (utilisateurRepository.existsByTelephone(telephone)) {
            return;
        }
        Utilisateur compte = compte(agriculteur.prenom(), agriculteur.nom(), agriculteur.telephone(), Role.AGRICULTEUR,
                agriculteur.statut(), motDePasseChiffre);
        compte.setEmail(adresseEmail(agriculteur.prenom(), agriculteur.nom()));
        Instant dateInscription = horloge.instant().minus(Duration.ofDays(agriculteur.ancienneteJours()));

        InfosAgriculteur infos = new InfosAgriculteur();
        infos.setCin(agriculteur.cin());
        infos.setDelegation(agriculteur.delegation());
        infos.setNumeroAdherent(agriculteur.numeroAdherent());
        infos.setMotifRefus(agriculteur.motifRefus());
        infos.setDateAcceptationConditions(dateInscription);
        compte.setAgriculteur(infos);
        utilisateurRepository.save(compte);

        // La date de création est remplie automatiquement au premier enregistrement : on la recule ensuite
        compte.setDateCreation(dateInscription);
        utilisateurRepository.save(compte);
        journal.info("Agriculteur de démonstration créé : {} {} ({})", agriculteur.prenom(), agriculteur.nom(),
                agriculteur.statut());
    }

    private Utilisateur compte(String prenom, String nom, String telephone, Role role, StatutCompte statut,
            String motDePasseChiffre) {
        Utilisateur compte = new Utilisateur();
        compte.setPrenom(prenom);
        compte.setNom(nom);
        compte.setTelephone(Identifiants.normaliserTelephone(telephone));
        compte.setMotDePasse(motDePasseChiffre);
        compte.setRole(role);
        compte.setStatut(statut);
        return compte;
    }

    /** « Ali » « Ben Salah » donne ali.bensalah@oliveconnect.tn */
    private static String adresseEmail(String prenom, String nom) {
        return (prenom + "." + nom.replace(" ", "")).toLowerCase() + "@oliveconnect.tn";
    }
}
