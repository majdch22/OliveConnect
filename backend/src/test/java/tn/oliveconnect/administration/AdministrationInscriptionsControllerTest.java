package tn.oliveconnect.administration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/** Traitement des inscriptions par l'administrateur : onglets, filtres, acceptation et refus. */
class AdministrationInscriptionsControllerTest extends TestAvecComptes {

    private static final String ADRESSE = "/api/administration/inscriptions";

    private Utilisateur nadia;
    private Utilisateur omar;
    private String jetonAdministrateur;

    @BeforeEach
    void preparerLesComptes() throws Exception {
        Utilisateur administrateur = creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF);
        nadia = creerAgriculteur("Nadia", "Kammoun", "+21655000210", "09876543", Delegation.AGAREB, StatutCompte.EN_ATTENTE_VALIDATION);
        omar = creerAgriculteur("Omar", "Fourati", "+21655000212", "11223355", Delegation.EL_HENCHA, StatutCompte.EN_ATTENTE_VALIDATION);
        creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.AGAREB, StatutCompte.ACTIF);
        creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        jetonAdministrateur = jetonDe(administrateur);
    }

    @Test
    void l_onglet_en_attente_ne_montre_que_les_agriculteurs_en_attente() throws Exception {
        mockMvc.perform(get(ADRESSE).param("tri", "NOM").param("sens", "ASC").header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.elements[*].nom", contains("Fourati", "Kammoun")));
    }

    @Test
    void filtre_par_delegation_dans_un_onglet() throws Exception {
        mockMvc.perform(get(ADRESSE).param("delegation", "AGAREB").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].prenom", contains("Nadia")));
    }

    @Test
    void accepter_active_le_compte_et_attribue_les_numeros_d_adherent_dans_l_ordre() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/accepter").header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("ACTIF")))
                .andExpect(jsonPath("$.numeroAdherent", is("ADH-0001")));

        mockMvc.perform(post(ADRESSE + "/" + omar.getId() + "/accepter").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.numeroAdherent", is("ADH-0002")));

        mockMvc.perform(get(ADRESSE).param("etat", "ACCEPTEE").param("tri", "NOM").param("sens", "ASC")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].prenom", contains("Ali", "Omar", "Nadia")));
    }

    @Test
    void un_agriculteur_accepte_peut_se_connecter() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/accepter").header("Authorization", jetonAdministrateur));

        mockMvc.perform(get("/api/profil").header("Authorization", jetonDe(nadia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom", is("Nadia")));
    }

    @Test
    void accepter_deux_fois_est_refuse() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/accepter").header("Authorization", jetonAdministrateur));

        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/accepter").header("Authorization", jetonAdministrateur))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSCRIPTION_DEJA_ACCEPTEE")));
    }

    @Test
    void refuser_sans_motif_est_impossible() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/refuser").header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motif").exists());
    }

    @Test
    void un_refus_garde_le_motif_et_empeche_la_connexion() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/refuser").header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Hors zone de collecte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("REFUSE")))
                .andExpect(jsonPath("$.motifRefus", is("Hors zone de collecte")));

        mockMvc.perform(post("/api/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"+21655000210\",\"motDePasse\":\"" + MOT_DE_PASSE + "\"}"))
                .andExpect(jsonPath("$.code", is("INSCRIPTION_REFUSEE")));
    }

    @Test
    void un_refus_peut_etre_annule_en_acceptant_l_inscription() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/refuser").header("Authorization", jetonAdministrateur)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Erreur\"}"));

        mockMvc.perform(post(ADRESSE + "/" + nadia.getId() + "/accepter").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.statut", is("ACTIF")))
                .andExpect(jsonPath("$.motifRefus").doesNotExist());
    }

    @Test
    void un_compte_du_personnel_n_est_pas_une_inscription() throws Exception {
        Utilisateur chef = utilisateurRepository.findByTelephone("+21698200002").orElseThrow();

        mockMvc.perform(post(ADRESSE + "/" + chef.getId() + "/accepter").header("Authorization", jetonAdministrateur))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PAS_UNE_INSCRIPTION")));
    }
}
