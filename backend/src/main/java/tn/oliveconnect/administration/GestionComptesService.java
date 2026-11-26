package tn.oliveconnect.administration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.referentiel.PressoirRepository;
import tn.oliveconnect.security.GenerateurMotDePasse;
import tn.oliveconnect.utilisateur.Identifiants;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.utilisateur.VerificationIdentifiants;

/**
 * Gestion des comptes par l'administrateur.
 *
 * Règles protégées ici :
 * - les agriculteurs s'inscrivent eux-mêmes ; l'administrateur crée les comptes du personnel et des autres administrateurs ;
 * - un compte agriculteur ne peut pas devenir un compte du personnel, et inversement (les données d'un agriculteur restent privées) ;
 * - un responsable du pressoir est toujours rattaché à un pressoir ;
 * - un administrateur ne peut ni se désactiver ni changer son propre rôle ;
 * - la plateforme garde toujours au moins un administrateur actif.
 */
@Service
@RequiredArgsConstructor
public class GestionComptesService {

    private final UtilisateurRepository utilisateurRepository;
    private final VerificationIdentifiants verificationIdentifiants;
    private final PasswordEncoder encodeurMotDePasse;
    private final GenerateurMotDePasse generateurMotDePasse;
    private final PressoirRepository pressoirRepository;

    public CompteAdministration consulter(String id) {
        return CompteAdministration.depuis(trouver(id));
    }

    public StatistiquesComptes statistiques() {
        List<StatistiquesComptes.NombreParRole> parRole = Arrays.stream(Role.values())
                .map(role -> new StatistiquesComptes.NombreParRole(role, role.getLibelle(),
                        utilisateurRepository.countByRoleAndStatut(role, StatutCompte.ACTIF)))
                .toList();
        return new StatistiquesComptes(
                utilisateurRepository.countByStatut(StatutCompte.EN_ATTENTE_VALIDATION),
                utilisateurRepository.countByStatut(StatutCompte.REFUSE),
                utilisateurRepository.countByStatut(StatutCompte.ACTIF),
                utilisateurRepository.countByStatut(StatutCompte.DESACTIVE),
                utilisateurRepository.count(),
                parRole);
    }

    public CompteEtMotDePasse creer(CompteRequete requete) {
        if (requete.role() == Role.AGRICULTEUR) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "ROLE_NON_AUTORISE",
                    "Les agriculteurs créent leur compte eux-mêmes depuis la page d'inscription.",
                    Map.of("role", "Choisissez un rôle du personnel ou administrateur."));
        }
        String telephone = telephoneValide(requete.telephone());
        String email = emailOuNull(requete.email());
        verificationIdentifiants.verifierDisponibles(telephone, email, null, null);

        String motDePasseProvisoire = generateurMotDePasse.generer();
        Utilisateur compte = new Utilisateur();
        compte.setNom(requete.nom().trim());
        compte.setPrenom(requete.prenom().trim());
        compte.setTelephone(telephone);
        compte.setEmail(email);
        compte.setRole(requete.role());
        compte.setPressoirId(pressoirValide(requete));
        compte.setStatut(StatutCompte.ACTIF);
        compte.setMotDePasse(encodeurMotDePasse.encode(motDePasseProvisoire));
        compte.setMotDePasseProvisoire(true);
        utilisateurRepository.save(compte);
        return new CompteEtMotDePasse(CompteAdministration.depuis(compte), motDePasseProvisoire);
    }

    public CompteAdministration modifier(String id, CompteRequete requete, String administrateurId) {
        Utilisateur compte = trouver(id);
        Role ancienRole = compte.getRole();
        Role nouveauRole = requete.role();

        if (ancienRole != nouveauRole) {
            if ((ancienRole == Role.AGRICULTEUR) != (nouveauRole == Role.AGRICULTEUR)) {
                throw refus("CHANGEMENT_ROLE_INTERDIT",
                        "Un compte agriculteur ne peut pas devenir un compte du personnel, et inversement.");
            }
            if (compte.getId().equals(administrateurId)) {
                throw refus("ACTION_SUR_SOI_INTERDITE", "Vous ne pouvez pas modifier votre propre rôle.");
            }
            verifierQuIlResteUnAdministrateur(compte);
        }

        String telephone = telephoneValide(requete.telephone());
        String email = emailOuNull(requete.email());
        verificationIdentifiants.verifierDisponibles(telephone, email, null, compte.getId());

        compte.setNom(requete.nom().trim());
        compte.setPrenom(requete.prenom().trim());
        compte.setTelephone(telephone);
        compte.setEmail(email);
        compte.setRole(nouveauRole);
        compte.setPressoirId(pressoirValide(requete));
        utilisateurRepository.save(compte);
        return CompteAdministration.depuis(compte);
    }

    /** Coupe l'accès : le compte est refusé dès sa prochaine requête, même s'il était connecté. */
    public CompteAdministration desactiver(String id, String administrateurId) {
        Utilisateur compte = trouver(id);
        if (compte.getId().equals(administrateurId)) {
            throw refus("ACTION_SUR_SOI_INTERDITE", "Vous ne pouvez pas désactiver votre propre compte.");
        }
        if (compte.getStatut() != StatutCompte.ACTIF) {
            throw refus("ETAT_INCOMPATIBLE", "Seul un compte actif peut être désactivé.");
        }
        verifierQuIlResteUnAdministrateur(compte);
        compte.setStatut(StatutCompte.DESACTIVE);
        utilisateurRepository.save(compte);
        return CompteAdministration.depuis(compte);
    }

    public CompteAdministration reactiver(String id) {
        Utilisateur compte = trouver(id);
        if (compte.getStatut() != StatutCompte.DESACTIVE) {
            throw refus("ETAT_INCOMPATIBLE",
                    "Seul un compte désactivé peut être réactivé. Les inscriptions se traitent dans « Inscriptions ».");
        }
        compte.setStatut(StatutCompte.ACTIF);
        utilisateurRepository.save(compte);
        return CompteAdministration.depuis(compte);
    }

    /** Donne un nouveau mot de passe provisoire et débloque le compte s'il était bloqué. */
    public CompteEtMotDePasse reinitialiserMotDePasse(String id) {
        Utilisateur compte = trouver(id);
        String motDePasseProvisoire = generateurMotDePasse.generer();
        compte.setMotDePasse(encodeurMotDePasse.encode(motDePasseProvisoire));
        compte.setMotDePasseProvisoire(true);
        compte.setTentativesEchouees(0);
        compte.setBloqueJusqua(null);
        utilisateurRepository.save(compte);
        return new CompteEtMotDePasse(CompteAdministration.depuis(compte), motDePasseProvisoire);
    }

    Utilisateur trouver(String id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ErreurMetierException(HttpStatus.NOT_FOUND, "COMPTE_INTROUVABLE",
                        "Ce compte n'existe pas."));
    }

    /** Refuse l'action si elle retirait le dernier administrateur actif. */
    private void verifierQuIlResteUnAdministrateur(Utilisateur compte) {
        boolean estAdministrateurActif = compte.getRole() == Role.ADMINISTRATEUR && compte.estActif();
        if (estAdministrateurActif
                && utilisateurRepository.countByRoleAndStatut(Role.ADMINISTRATEUR, StatutCompte.ACTIF) <= 1) {
            throw refus("DERNIER_ADMINISTRATEUR", "La plateforme doit garder au moins un administrateur actif.");
        }
    }

    /**
     * Un responsable du pressoir est rattaché à un pressoir existant et non archivé ;
     * les autres rôles n'en ont pas.
     */
    private String pressoirValide(CompteRequete requete) {
        if (requete.role() != Role.RESPONSABLE_PRESSOIR) {
            return null;
        }
        if (requete.pressoirId() == null || !pressoirRepository.existsByIdAndArchiveFalse(requete.pressoirId())) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.",
                    Map.of("pressoirId", "Choisissez le pressoir de ce responsable."));
        }
        return requete.pressoirId();
    }

    private String telephoneValide(String saisie) {
        String telephone = Identifiants.normaliserTelephone(saisie);
        if (!VerificationIdentifiants.estTelephoneTunisien(telephone)) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.",
                    Map.of("telephone", "Saisissez un numéro tunisien à 8 chiffres."));
        }
        return telephone;
    }

    private static String emailOuNull(String saisie) {
        return saisie == null || saisie.isBlank() ? null : Identifiants.normaliserEmail(saisie);
    }

    private static ErreurMetierException refus(String code, String message) {
        return new ErreurMetierException(HttpStatus.CONFLICT, code, message);
    }
}
