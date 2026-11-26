package tn.oliveconnect.parametres;

import static org.hamcrest.Matchers.containsString;
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

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/** Paramètres de la plateforme : lecture, validation des bornes et application immédiate. */
class ParametresControllerTest extends TestAvecComptes {

    private static final String ADRESSE = "/api/administration/parametres";

    private String jetonAdministrateur;

    @BeforeEach
    void preparer() throws Exception {
        jetonAdministrateur = jetonDe(creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF));
    }

    @Test
    void les_valeurs_par_defaut_sont_creees_au_premier_acces() throws Exception {
        mockMvc.perform(get(ADRESSE).header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres.regles.seuilAlerteMaturite", is(70)))
                .andExpect(jsonPath("$.parametres.regles.tolerancePesee", is(110)))
                .andExpect(jsonPath("$.parametres.securite.tentativesAvantBlocage", is(5)))
                .andExpect(jsonPath("$.frequencesPossibles", hasSize(FrequenceImport.values().length)));
    }

    @Test
    void les_autres_roles_n_ont_pas_acces_aux_parametres() throws Exception {
        String jetonAgriculteur = jetonDe(creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567",
                Delegation.AGAREB, StatutCompte.ACTIF));

        mockMvc.perform(get(ADRESSE).header("Authorization", jetonAgriculteur)).andExpect(status().isForbidden());
    }

    @Test
    void une_modification_est_enregistree_avec_son_auteur() throws Exception {
        mockMvc.perform(put(ADRESSE).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content(parametres(75, 3, "2026-11-15", "2027-02-28")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres.regles.seuilAlerteMaturite", is(75)))
                .andExpect(jsonPath("$.parametres.modifiePar", is("Admin Principal")));
    }

    @Test
    void le_nombre_de_tentatives_modifie_s_applique_immediatement_a_la_connexion() throws Exception {
        creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.AGAREB, StatutCompte.ACTIF);
        mockMvc.perform(put(ADRESSE).header("Authorization", jetonAdministrateur)
                .contentType(MediaType.APPLICATION_JSON).content(parametres(70, 3, "2026-11-15", "2027-02-28")));

        mockMvc.perform(post("/api/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"+21698123412\",\"motDePasse\":\"mauvais\"}"))
                .andExpect(jsonPath("$.message", containsString("Il vous reste 2 tentative(s)")));
    }

    @Test
    void une_valeur_hors_des_bornes_est_refusee_avec_le_champ_concerne() throws Exception {
        mockMvc.perform(put(ADRESSE).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content(parametres(20, 5, "2026-11-15", "2027-02-28")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs['regles.seuilAlerteMaturite']").exists());
    }

    @Test
    void la_fin_de_campagne_doit_etre_apres_son_debut() throws Exception {
        mockMvc.perform(put(ADRESSE).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content(parametres(70, 5, "2027-02-28", "2026-11-15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs['campagne.fin']").exists());
    }

    private static String parametres(int seuilMaturite, int tentatives, String debut, String fin) {
        return """
                {"campagne":{"debut":"%s","fin":"%s"},
                 "regles":{"seuilAlerteMaturite":%d,"tolerancePesee":110,"distanceMaximaleSignalement":500,"delaiCibleUrgences":24},
                 "imports":{"referentiels":"CHAQUE_JOUR","meteoEtMaturite":"TOUTES_LES_15_MINUTES","capacitePressoirs":"TOUTES_LES_15_MINUTES"},
                 "notifications":{"smsAgriculteurs":true,"emailRecapitulatifQuotidien":false},
                 "securite":{"dureeSessionHeures":8,"tentativesAvantBlocage":%d,"dureeBlocageMinutes":15}}
                """.formatted(debut, fin, seuilMaturite, tentatives);
    }
}
