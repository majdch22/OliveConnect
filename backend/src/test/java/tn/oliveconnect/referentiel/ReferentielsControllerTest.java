package tn.oliveconnect.referentiel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/** Référentiels : variétés et pressoirs (création, recherche, tri, archivage), catégories de qualité. */
class ReferentielsControllerTest extends TestAvecComptes {

    private static final String VARIETES = "/api/administration/referentiels/varietes";
    private static final String PRESSOIRS = "/api/administration/referentiels/pressoirs";

    private String jetonAdministrateur;

    @BeforeEach
    void preparer() throws Exception {
        jetonAdministrateur = jetonDe(creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF));
    }

    // --- Variétés ---

    @Test
    void une_variete_creee_recoit_un_code_et_sa_saison_en_toutes_lettres() throws Exception {
        creerVariete("Chemlali", 3000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("VAR-01")))
                .andExpect(jsonPath("$.saison", is("Novembre → Février")));
        creerVariete("Chetoui", 3500).andExpect(jsonPath("$.code", is("VAR-02")));
    }

    @Test
    void deux_varietes_ne_peuvent_pas_porter_le_meme_nom() throws Exception {
        creerVariete("Chemlali", 3000);

        creerVariete("chemlali", 2000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champs.nom").exists());
    }

    @Test
    void les_varietes_se_trient_par_rendement_et_se_recherchent_par_nom() throws Exception {
        creerVariete("Chemlali", 3000);
        creerVariete("Chetoui", 3500);
        creerVariete("Zarrazi", 2600);

        mockMvc.perform(get(VARIETES).param("tri", "RENDEMENT").param("sens", "DESC").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$[*].nom", contains("Chetoui", "Chemlali", "Zarrazi")));
        mockMvc.perform(get(VARIETES).param("texte", "che").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$[*].nom", contains("Chemlali", "Chetoui")));
    }

    @Test
    void une_variete_archivee_disparait_de_la_liste_sauf_si_on_demande_les_archives() throws Exception {
        String id = JsonPath.read(creerVariete("Chemlali", 3000).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post(VARIETES + "/" + id + "/archiver").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.archivee", is(true)));

        mockMvc.perform(get(VARIETES).header("Authorization", jetonAdministrateur)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get(VARIETES).param("avecArchives", "true").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // --- Pressoirs ---

    @Test
    void un_pressoir_garde_sa_position_et_recoit_un_code() throws Exception {
        creerPressoir("Huilerie El Amra", 34.99, 10.92)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("PR-01")))
                .andExpect(jsonPath("$.latitude", closeTo(34.99, 0.0001)))
                .andExpect(jsonPath("$.longitude", closeTo(10.92, 0.0001)))
                .andExpect(jsonPath("$.libelleDelegation", is("El Amra")));
    }

    @Test
    void une_position_hors_de_la_region_de_sfax_est_refusee() throws Exception {
        creerPressoir("Pressoir de Tunis", 36.80, 10.18)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.latitude").exists());
    }

    @Test
    void un_responsable_du_pressoir_doit_etre_rattache_a_un_pressoir_et_bloque_son_archivage() throws Exception {
        String pressoirId = JsonPath.read(creerPressoir("Huilerie El Amra", 34.99, 10.92).andReturn().getResponse()
                .getContentAsString(), "$.id");

        mockMvc.perform(post("/api/administration/utilisateurs").header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responsable(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.pressoirId").exists());

        mockMvc.perform(post("/api/administration/utilisateurs").header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responsable(pressoirId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compte.pressoirId", is(pressoirId)));

        mockMvc.perform(get(PRESSOIRS).header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$[0].nombreResponsables", is(1)));
        mockMvc.perform(post(PRESSOIRS + "/" + pressoirId + "/archiver").header("Authorization", jetonAdministrateur))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PRESSOIR_ENCORE_UTILISE")));
    }

    @Test
    void modifier_un_pressoir_change_sa_capacite() throws Exception {
        String id = JsonPath.read(creerPressoir("Huilerie El Amra", 34.99, 10.92).andReturn().getResponse()
                .getContentAsString(), "$.id");

        mockMvc.perform(put(PRESSOIRS + "/" + id).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pressoir("Huilerie El Amra", 34.99, 10.92).replace("20000", "30000")))
                .andExpect(jsonPath("$.capaciteJournaliereKg", is(30000)));
    }

    // --- Catégories de qualité ---

    @Test
    void les_categories_de_qualite_sont_fixes_et_decrites() throws Exception {
        mockMvc.perform(get("/api/administration/referentiels/categories-qualite").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].libelle", is("Extra vierge")));
    }

    @Test
    void la_categorie_se_deduit_de_l_acidite() {
        assertThat(CategorieQualite.pourAcidite(0.6)).isEqualTo(CategorieQualite.EXTRA_VIERGE);
        assertThat(CategorieQualite.pourAcidite(1.5)).isEqualTo(CategorieQualite.VIERGE);
        assertThat(CategorieQualite.pourAcidite(4.0)).isEqualTo(CategorieQualite.LAMPANTE);
    }

    // --- Outils des tests ---

    private ResultActions creerVariete(String nom, int rendement) throws Exception {
        return mockMvc.perform(post(VARIETES).header("Authorization", jetonAdministrateur).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"%s\",\"rendementMoyenKgParHectare\":%d,\"moisDebutRecolte\":11,\"moisFinRecolte\":2}"
                        .formatted(nom, rendement)));
    }

    private ResultActions creerPressoir(String nom, double latitude, double longitude)
            throws Exception {
        return mockMvc.perform(post(PRESSOIRS).header("Authorization", jetonAdministrateur)
                .contentType(MediaType.APPLICATION_JSON).content(pressoir(nom, latitude, longitude)));
    }

    private static String pressoir(String nom, double latitude, double longitude) {
        return """
                {"nom":"%s","delegation":"EL_AMRA","latitude":%s,"longitude":%s,"capaciteJournaliereKg":20000,"telephone":null}
                """.formatted(nom, latitude, longitude);
    }

    private static String responsable(String pressoirId) {
        String champPressoir = pressoirId == null ? "null" : "\"" + pressoirId + "\"";
        return """
                {"prenom":"Fethi","nom":"Ayadi","telephone":"98200004","email":null,"role":"RESPONSABLE_PRESSOIR","pressoirId":%s}
                """.formatted(champPressoir);
    }
}
