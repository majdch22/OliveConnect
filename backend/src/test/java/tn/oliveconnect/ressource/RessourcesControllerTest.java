package tn.oliveconnect.ressource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/** Ressources et équipes : règles par type, filtres, tri, archivage. */
class RessourcesControllerTest extends TestAvecComptes {

    private String jetonAdministrateur;

    @BeforeEach
    void preparer() throws Exception {
        mongoTemplate.remove(new Query(), Ressource.class);
        mongoTemplate.remove(new Query(), Equipe.class);
        jetonAdministrateur = jetonDe(creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF));
    }

    @Test
    void chaque_type_recoit_son_propre_code() throws Exception {
        creer(ressource("TRACTEUR", "New Holland TD5", null, null, null, "DISPONIBLE")).andExpect(jsonPath("$.code", is("TR-01")));
        creer(ressource("BENNE", "Benne 2 t", 2000, null, null, "DISPONIBLE")).andExpect(jsonPath("$.code", is("BN-01")));
        creer(ressource("OUVRIER", "Youssef Hammami", null, null, null, "DISPONIBLE")).andExpect(jsonPath("$.code", is("OUV-001")));
        creer(ressource("MATERIEL", "Filets", null, 40, null, "DISPONIBLE")).andExpect(jsonPath("$.code", is("MAT-01")));
    }

    @Test
    void une_benne_sans_capacite_est_refusee() throws Exception {
        creer(ressource("BENNE", "Benne", null, null, null, "DISPONIBLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.capaciteKg").exists());
    }

    @Test
    void un_materiel_sans_quantite_est_refuse() throws Exception {
        creer(ressource("MATERIEL", "Filets", null, null, null, "DISPONIBLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.quantite").exists());
    }

    @Test
    void seul_un_ouvrier_peut_appartenir_a_une_equipe() throws Exception {
        String equipe = creerEquipe("Équipe A");

        creer(ressource("TRACTEUR", "Kubota", null, null, equipe, "DISPONIBLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.equipeId").exists());
        creer(ressource("OUVRIER", "Bilel Gharbi", null, null, equipe, "DISPONIBLE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomEquipe", is("Équipe A")));
    }

    @Test
    void une_equipe_compte_ses_membres_et_ceux_qui_sont_disponibles() throws Exception {
        String equipe = creerEquipe("Équipe A");
        creer(ressource("OUVRIER", "Youssef", null, null, equipe, "DISPONIBLE"));
        creer(ressource("OUVRIER", "Bilel", null, null, equipe, "DISPONIBLE"));
        creer(ressource("OUVRIER", "Slim", null, null, equipe, "EN_CONGE"));

        mockMvc.perform(get("/api/equipes").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$[0].nombreMembres", is(3)))
                .andExpect(jsonPath("$[0].nombreDisponibles", is(2)));
        mockMvc.perform(post("/api/equipes/" + equipe + "/archiver").header("Authorization", jetonAdministrateur))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EQUIPE_NON_VIDE")));
    }

    @Test
    void la_liste_se_filtre_par_type_et_etat_et_se_trie_par_capacite() throws Exception {
        creer(ressource("BENNE", "Benne 2 t", 2000, null, null, "DISPONIBLE"));
        creer(ressource("BENNE", "Benne 3 t", 3000, null, null, "DISPONIBLE"));
        creer(ressource("BENNE", "Benne 2 t bis", 2000, null, null, "EN_REVISION"));
        creer(ressource("TRACTEUR", "Kubota", null, null, null, "DISPONIBLE"));

        mockMvc.perform(get("/api/ressources").param("type", "BENNE").param("etat", "DISPONIBLE")
                        .param("tri", "CAPACITE").param("sens", "DESC").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.elements[*].capaciteKg", contains(3000, 2000)));
        mockMvc.perform(get("/api/ressources/resume").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$[?(@.type == 'BENNE')].disponibles", contains(2)));
    }

    @Test
    void le_type_ne_change_pas_et_les_champs_sans_objet_sont_vides() throws Exception {
        String id = JsonPath.read(creer(ressource("TRACTEUR", "Kubota", null, null, null, "DISPONIBLE"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/ressources/" + id).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content(ressource("BENNE", "Kubota", 2000, null, null, "DISPONIBLE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.type").exists());
        mockMvc.perform(put("/api/ressources/" + id).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content(ressource("TRACTEUR", "Kubota M7060", 2000, 5, null, "EN_PANNE")))
                .andExpect(jsonPath("$.etat", is("EN_PANNE")))
                .andExpect(jsonPath("$.capaciteKg", nullValue()))
                .andExpect(jsonPath("$.quantite", nullValue()));
    }

    @Test
    void une_ressource_archivee_disparait_de_la_liste() throws Exception {
        String id = JsonPath.read(creer(ressource("TRACTEUR", "Kubota", null, null, null, "DISPONIBLE"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/ressources/" + id + "/archiver").header("Authorization", jetonAdministrateur));

        mockMvc.perform(get("/api/ressources").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements", hasSize(0)));
    }

    @Test
    void le_responsable_logistique_a_acces_mais_pas_le_chef_d_equipe() throws Exception {
        String logistique = jetonDe(creerPersonnel("Rim", "Gharbi", "+21698200003", Role.RESPONSABLE_LOGISTIQUE, StatutCompte.ACTIF));
        String chef = jetonDe(creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF));

        mockMvc.perform(get("/api/ressources").header("Authorization", logistique)).andExpect(status().isOk());
        mockMvc.perform(get("/api/ressources").header("Authorization", chef)).andExpect(status().isForbidden());
    }

    // --- Outils des tests ---

    private ResultActions creer(String corps) throws Exception {
        return mockMvc.perform(post("/api/ressources").header("Authorization", jetonAdministrateur)
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private String creerEquipe(String nom) throws Exception {
        String reponse = mockMvc.perform(post("/api/equipes").header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"" + nom + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reponse, "$.id");
    }

    private static String ressource(String type, String nom, Integer capacite, Integer quantite, String equipeId, String etat) {
        return """
                {"type":"%s","nom":"%s","matricule":null,"capaciteKg":%s,"quantite":%s,"equipeId":%s,"telephone":null,"etat":"%s"}
                """.formatted(type, nom, capacite, quantite, equipeId == null ? "null" : "\"" + equipeId + "\"", etat);
    }
}
