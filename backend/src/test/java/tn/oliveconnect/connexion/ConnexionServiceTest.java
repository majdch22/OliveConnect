package tn.oliveconnect.connexion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.configuration.OliveConnectProperties;
import tn.oliveconnect.security.JetonService;
import tn.oliveconnect.security.JetonService.JetonGenere;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/** Tests unitaires du service de connexion : la base et le jeton sont simulés (Mockito). */
@ExtendWith(MockitoExtension.class)
class ConnexionServiceTest {

    private static final String MOT_DE_PASSE = "Olive2026!";
    private static final String EMAIL = "ali@exemple.tn";
    private static final Instant MAINTENANT = Instant.parse("2026-12-02T08:00:00Z");

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private JetonService jetonService;

    private final PasswordEncoder encodeurMotDePasse = new BCryptPasswordEncoder();
    private ConnexionService connexionService;

    @BeforeEach
    void preparer() {
        OliveConnectProperties proprietes = new OliveConnectProperties(
                new OliveConnectProperties.Securite("secret-inutilise-dans-ce-test-unitaire",
                        Duration.ofHours(8), 5, Duration.ofMinutes(15)),
                null);
        Clock horlogeFixe = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Tunis"));
        connexionService = new ConnexionService(utilisateurRepository, encodeurMotDePasse, jetonService,
                proprietes, horlogeFixe);
    }

    @Test
    void connexion_reussie_renvoie_un_jeton_et_le_role() {
        Utilisateur agriculteur = compte(Role.AGRICULTEUR, StatutCompte.ACTIF);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(agriculteur));
        when(jetonService.generer(agriculteur)).thenReturn(new JetonGenere("jeton-signe", MAINTENANT.plusSeconds(3600)));

        ConnexionReponse reponse = connexionService.connecter(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS);

        assertThat(reponse.jeton()).isEqualTo("jeton-signe");
        assertThat(reponse.role()).isEqualTo(Role.AGRICULTEUR);
        assertThat(agriculteur.getDerniereConnexion()).isEqualTo(MAINTENANT);
    }

    @Test
    void connexion_refusee_si_le_compte_n_existe_pas() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertRefus(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS, "IDENTIFIANTS_INCORRECTS");
    }

    @Test
    void connexion_refusee_si_mot_de_passe_incorrect_et_tentatives_restantes_indiquees() {
        Utilisateur agriculteur = compte(Role.AGRICULTEUR, StatutCompte.ACTIF);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(agriculteur));

        assertThatThrownBy(() -> connexionService.connecter(requete("mauvais"), EspaceConnexion.UTILISATEURS))
                .isInstanceOf(ConnexionRefuseeException.class)
                .hasMessageContaining("Il vous reste 4 tentative(s)");
        assertThat(agriculteur.getTentativesEchouees()).isEqualTo(1);
        verify(utilisateurRepository).save(agriculteur);
    }

    @Test
    void le_cinquieme_mot_de_passe_incorrect_bloque_le_compte() {
        Utilisateur agriculteur = compte(Role.AGRICULTEUR, StatutCompte.ACTIF);
        agriculteur.setTentativesEchouees(4);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(agriculteur));

        assertRefus(requete("mauvais"), EspaceConnexion.UTILISATEURS, "COMPTE_BLOQUE");
        assertThat(agriculteur.estBloque(MAINTENANT)).isTrue();
    }

    @Test
    void compte_bloque_refuse_meme_avec_le_bon_mot_de_passe() {
        Utilisateur agriculteur = compte(Role.AGRICULTEUR, StatutCompte.ACTIF);
        agriculteur.setBloqueJusqua(MAINTENANT.plus(Duration.ofMinutes(10)));
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(agriculteur));

        assertThatThrownBy(() -> connexionService.connecter(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS))
                .hasMessageContaining("Réessayez dans 10 minute(s)");
        verify(jetonService, never()).generer(any());
    }

    @Test
    void compte_en_attente_de_validation_refuse() {
        when(utilisateurRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(compte(Role.AGRICULTEUR, StatutCompte.EN_ATTENTE_VALIDATION)));

        assertRefus(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS, "COMPTE_EN_ATTENTE_VALIDATION");
    }

    @Test
    void compte_desactive_refuse() {
        when(utilisateurRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(compte(Role.AGRICULTEUR, StatutCompte.DESACTIVE)));

        assertRefus(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS, "COMPTE_DESACTIVE");
    }

    @Test
    void inscription_refusee_ne_peut_pas_se_connecter() {
        when(utilisateurRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(compte(Role.AGRICULTEUR, StatutCompte.REFUSE)));

        assertRefus(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS, "INSCRIPTION_REFUSEE");
    }

    @Test
    void l_administrateur_ne_peut_pas_utiliser_la_page_de_connexion_generale() {
        when(utilisateurRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(compte(Role.ADMINISTRATEUR, StatutCompte.ACTIF)));

        assertRefus(requete(MOT_DE_PASSE), EspaceConnexion.UTILISATEURS, "MAUVAIS_ESPACE_CONNEXION");
    }

    @Test
    void un_agriculteur_ne_peut_pas_utiliser_la_page_administrateur() {
        when(utilisateurRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(compte(Role.AGRICULTEUR, StatutCompte.ACTIF)));

        assertRefus(requete(MOT_DE_PASSE), EspaceConnexion.ADMINISTRATION, "MAUVAIS_ESPACE_CONNEXION");
    }

    @Test
    void connexion_possible_avec_le_telephone_ecrit_sans_indicatif() {
        Utilisateur agriculteur = compte(Role.AGRICULTEUR, StatutCompte.ACTIF);
        when(utilisateurRepository.findByTelephone("+21698123412")).thenReturn(Optional.of(agriculteur));
        when(jetonService.generer(agriculteur)).thenReturn(new JetonGenere("jeton-signe", MAINTENANT));

        ConnexionReponse reponse = connexionService.connecter(
                new ConnexionRequete("98 123 412", MOT_DE_PASSE), EspaceConnexion.UTILISATEURS);

        assertThat(reponse.jeton()).isEqualTo("jeton-signe");
    }

    // --- Outils des tests ---

    private Utilisateur compte(Role role, StatutCompte statut) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId("compte-1");
        utilisateur.setPrenom("Ali");
        utilisateur.setNom("Ben Salah");
        utilisateur.setEmail(EMAIL);
        utilisateur.setMotDePasse(encodeurMotDePasse.encode(MOT_DE_PASSE));
        utilisateur.setRole(role);
        utilisateur.setStatut(statut);
        return utilisateur;
    }

    private ConnexionRequete requete(String motDePasse) {
        return new ConnexionRequete(EMAIL, motDePasse);
    }

    private void assertRefus(ConnexionRequete requete, EspaceConnexion espace, String codeAttendu) {
        assertThatThrownBy(() -> connexionService.connecter(requete, espace))
                .isInstanceOf(ConnexionRefuseeException.class)
                .extracting(erreur -> ((ErreurMetierException) erreur).getCode())
                .isEqualTo(codeAttendu);
    }
}
