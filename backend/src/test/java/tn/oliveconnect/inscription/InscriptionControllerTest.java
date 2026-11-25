package tn.oliveconnect.inscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/** Inscription d'un agriculteur : vraie requête, vraie base MongoDB de test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @BeforeEach
    void viderLaBase() {
        utilisateurRepository.deleteAll();
    }

    @Test
    void une_inscription_valide_cree_un_compte_agriculteur_en_attente() throws Exception {
        mockMvc.perform(inscription(formulaire("98 123 412", "ali@exemple.tn", "01234567", "Olive2026", "Olive2026", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prenom").value("Ali"))
                .andExpect(jsonPath("$.telephoneMasque").value("+216 98 ••• 412"));

        Utilisateur enBase = utilisateurRepository.findByTelephone("+21698123412").orElseThrow();
        assertThat(enBase.getRole()).isEqualTo(Role.AGRICULTEUR);
        assertThat(enBase.getStatut()).isEqualTo(StatutCompte.EN_ATTENTE_VALIDATION);
        assertThat(enBase.getAgriculteur().getCin()).isEqualTo("01234567");
        assertThat(enBase.getAgriculteur().getDelegation()).isEqualTo(Delegation.SAKIET_EZZIT);
        assertThat(enBase.getAgriculteur().getDateAcceptationConditions()).isNotNull();
        assertThat(enBase.getMotDePasse()).isNotEqualTo("Olive2026").startsWith("$2");
    }

    @Test
    void apres_inscription_la_connexion_indique_que_le_compte_est_en_attente() throws Exception {
        mockMvc.perform(inscription(formulaire("98123412", null, "01234567", "Olive2026", "Olive2026", true)));

        mockMvc.perform(post("/api/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"98123412\",\"motDePasse\":\"Olive2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPTE_EN_ATTENTE_VALIDATION"));
    }

    @Test
    void l_email_est_facultatif() throws Exception {
        mockMvc.perform(inscription(formulaire("98123412", "", "01234567", "Olive2026", "Olive2026", true)))
                .andExpect(status().isCreated());

        assertThat(utilisateurRepository.findByTelephone("+21698123412").orElseThrow().getEmail()).isNull();
    }

    @Test
    void telephone_et_carte_d_identite_deja_utilises_sont_refuses_avec_le_detail_par_champ() throws Exception {
        mockMvc.perform(inscription(formulaire("98123412", null, "01234567", "Olive2026", "Olive2026", true)));

        mockMvc.perform(inscription(formulaire("+216 98 123 412", null, "01234567", "Olive2026", "Olive2026", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DONNEES_DEJA_UTILISEES"))
                .andExpect(jsonPath("$.champs.telephone").exists())
                .andExpect(jsonPath("$.champs.cin").exists());
    }

    @Test
    void mots_de_passe_differents_sont_refuses() throws Exception {
        mockMvc.perform(inscription(formulaire("98123412", null, "01234567", "Olive2026", "Olive2027", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.confirmationMotDePasse").exists());
    }

    @Test
    void conditions_non_acceptees_et_champs_incorrects_sont_refuses() throws Exception {
        mockMvc.perform(inscription(formulaire("98123412", "pas-un-email", "123", "court", "court", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.conditionsAcceptees").exists())
                .andExpect(jsonPath("$.champs.email").exists())
                .andExpect(jsonPath("$.champs.cin").exists())
                .andExpect(jsonPath("$.champs.motDePasse").exists());
    }

    @Test
    void un_numero_qui_n_est_pas_tunisien_est_refuse() throws Exception {
        mockMvc.perform(inscription(formulaire("12345", null, "01234567", "Olive2026", "Olive2026", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.telephone").exists());
    }

    @Test
    void la_liste_des_delegations_est_publique() throws Exception {
        mockMvc.perform(get("/api/inscription/delegations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(Delegation.values().length)))
                .andExpect(jsonPath("$[0].libelle").value("Sfax Ville"));
    }

    // --- Outils des tests ---

    private RequestBuilder inscription(String corps) {
        return post("/api/inscription").contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    private String formulaire(String telephone, String email, String cin, String motDePasse, String confirmation,
            boolean conditionsAcceptees) {
        String champEmail = email == null ? "null" : "\"" + email + "\"";
        return """
                {"nom":"Ben Salah","prenom":"Ali","cin":"%s","telephone":"%s","email":%s,
                 "delegation":"SAKIET_EZZIT","motDePasse":"%s","confirmationMotDePasse":"%s","conditionsAcceptees":%s}
                """.formatted(cin, telephone, champEmail, motDePasse, confirmation, conditionsAcceptees);
    }
}
