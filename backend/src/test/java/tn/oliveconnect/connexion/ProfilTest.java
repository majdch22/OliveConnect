package tn.oliveconnect.connexion;

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

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.StatutCompte;

/** « Mon profil » (UC07) : changer ses coordonnées, ses préférences, et ne plus revoir la visite guidée. */
class ProfilTest extends TestAvecComptes {

    private String jetonAli;

    @BeforeEach
    void preparer() throws Exception {
        jetonAli = jetonDe(creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF));
        creerAgriculteur("Hela", "Frikha", "+21697111333", "03456789", Delegation.AGAREB, StatutCompte.ACTIF);
    }

    @Test
    void changer_ses_coordonnees_avec_son_mot_de_passe() throws Exception {
        changerCoordonnees("55 444 333", "ali.nouveau@exemple.tn", MOT_DE_PASSE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone", is("+21655444333")))
                .andExpect(jsonPath("$.delegation", is("AGAREB")));

        jetonDe("ali.nouveau@exemple.tn", MOT_DE_PASSE, false);
    }

    @Test
    void sans_le_bon_mot_de_passe_rien_ne_change() throws Exception {
        changerCoordonnees("55444333", null, "MauvaisMot1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motDePasse").exists());
    }

    @Test
    void le_telephone_d_un_autre_compte_est_refuse() throws Exception {
        changerCoordonnees("97111333", null, MOT_DE_PASSE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champs.telephone").exists());
    }

    @Test
    void la_visite_guidee_terminee_n_est_plus_proposee() throws Exception {
        mockMvc.perform(get("/api/profil").header("Authorization", jetonAli)).andExpect(jsonPath("$.visiteGuideeTerminee", is(false)));

        mockMvc.perform(post("/api/profil/visite-guidee").header("Authorization", jetonAli))
                .andExpect(jsonPath("$.visiteGuideeTerminee", is(true)));
    }

    private ResultActions changerCoordonnees(String telephone, String email, String motDePasse) throws Exception {
        String champEmail = email == null ? "null" : "\"" + email + "\"";
        return mockMvc.perform(put("/api/profil/coordonnees").header("Authorization", jetonAli)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telephone\":\"%s\",\"email\":%s,\"delegation\":\"AGAREB\",\"motDePasse\":\"%s\"}"
                        .formatted(telephone, champEmail, motDePasse)));
    }
}
