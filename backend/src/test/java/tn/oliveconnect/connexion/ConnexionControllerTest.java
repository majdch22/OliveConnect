package tn.oliveconnect.connexion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Tests de bout en bout de la connexion : vraie requête web, vraie sécurité, vraie base MongoDB de test.
 * Vérifie l'interaction entre le serveur et la base de données (scénarios nominaux et alternatifs).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConnexionControllerTest {

    private static final String MOT_DE_PASSE = "Olive2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder encodeurMotDePasse;

    @BeforeEach
    void preparerLaBase() {
        utilisateurRepository.deleteAll();
        enregistrer("agriculteur@test.tn", "+21698000001", Role.AGRICULTEUR, StatutCompte.ACTIF);
        enregistrer("administrateur@test.tn", "+21698000002", Role.ADMINISTRATEUR, StatutCompte.ACTIF);
        enregistrer("en.attente@test.tn", "+21698000003", Role.AGRICULTEUR, StatutCompte.EN_ATTENTE_VALIDATION);
    }

    @Test
    void un_agriculteur_se_connecte_et_recoit_un_jeton() throws Exception {
        mockMvc.perform(connexion("/api/connexion", "agriculteur@test.tn", MOT_DE_PASSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton", notNullValue()))
                .andExpect(jsonPath("$.role").value("AGRICULTEUR"))
                .andExpect(jsonPath("$.libelleRole").value("Agriculteur"));
    }

    @Test
    void l_administrateur_se_connecte_sur_sa_propre_page() throws Exception {
        mockMvc.perform(connexion("/api/administration/connexion", "administrateur@test.tn", MOT_DE_PASSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMINISTRATEUR"));
    }

    @Test
    void l_administrateur_est_refuse_sur_la_page_generale() throws Exception {
        mockMvc.perform(connexion("/api/connexion", "administrateur@test.tn", MOT_DE_PASSE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MAUVAIS_ESPACE_CONNEXION"));
    }

    @Test
    void mot_de_passe_incorrect_renvoie_401_et_compte_l_echec_en_base() throws Exception {
        mockMvc.perform(connexion("/api/connexion", "agriculteur@test.tn", "mauvais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTIFIANTS_INCORRECTS"))
                .andExpect(jsonPath("$.message", containsString("Il vous reste 4")));

        Utilisateur enBase = utilisateurRepository.findByEmail("agriculteur@test.tn").orElseThrow();
        assertThat(enBase.getTentativesEchouees()).isEqualTo(1);
    }

    @Test
    void compte_en_attente_renvoie_403_avec_un_message_clair() throws Exception {
        mockMvc.perform(connexion("/api/connexion", "en.attente@test.tn", MOT_DE_PASSE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPTE_EN_ATTENTE_VALIDATION"));
    }

    @Test
    void champs_vides_renvoient_400_avec_le_message_de_chaque_champ() throws Exception {
        mockMvc.perform(post("/api/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"\",\"motDePasse\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DONNEES_INVALIDES"))
                .andExpect(jsonPath("$.champs.identifiant", notNullValue()))
                .andExpect(jsonPath("$.champs.motDePasse", notNullValue()));
    }

    @Test
    void le_jeton_donne_acces_au_profil() throws Exception {
        MvcResult resultat = mockMvc.perform(connexion("/api/connexion", "agriculteur@test.tn", MOT_DE_PASSE))
                .andReturn();
        String jeton = JsonPath.read(resultat.getResponse().getContentAsString(), "$.jeton");

        mockMvc.perform(get("/api/profil").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("agriculteur@test.tn"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
    }

    @Test
    void le_profil_est_refuse_sans_jeton() throws Exception {
        mockMvc.perform(get("/api/profil"))
                .andExpect(status().isUnauthorized());
    }

    // --- Outils des tests ---

    private RequestBuilder connexion(String adresse, String identifiant, String motDePasse) {
        String corps = "{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"" + motDePasse + "\"}";
        return post(adresse).contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    private void enregistrer(String email, String telephone, Role role, StatutCompte statut) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom("Test");
        utilisateur.setPrenom("Compte");
        utilisateur.setEmail(email);
        utilisateur.setTelephone(telephone);
        utilisateur.setMotDePasse(encodeurMotDePasse.encode(MOT_DE_PASSE));
        utilisateur.setRole(role);
        utilisateur.setStatut(statut);
        utilisateurRepository.save(utilisateur);
    }
}
