package tn.oliveconnect.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/** Gestion des comptes par l'administrateur : liste, filtres, tri, pagination et actions. */
class AdministrationComptesControllerTest extends TestAvecComptes {

    private static final String ADRESSE = "/api/administration/utilisateurs";

    private Utilisateur administrateur;
    private Utilisateur chefEquipe;
    private String jetonAdministrateur;

    @BeforeEach
    void preparerLesComptes() throws Exception {
        administrateur = creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF);
        chefEquipe = creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        creerPersonnel("Salma", "Chaabane", "+21698200001", Role.RESPONSABLE_COLLECTE, StatutCompte.ACTIF);
        creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);
        creerAgriculteur("Hamza", "Jlassi", "+21655000211", "07654321", Delegation.EL_HENCHA, StatutCompte.DESACTIVE);
        creerAgriculteur("Nadia", "Kammoun", "+21655000210", "09876543", Delegation.AGAREB, StatutCompte.EN_ATTENTE_VALIDATION);
        jetonAdministrateur = jetonDe(administrateur);
    }

    // --- Accès ---

    @Test
    void la_liste_est_refusee_sans_jeton() throws Exception {
        mockMvc.perform(get(ADRESSE)).andExpect(status().isUnauthorized());
    }

    @Test
    void la_liste_est_refusee_aux_autres_roles() throws Exception {
        mockMvc.perform(get(ADRESSE).header("Authorization", jetonDe(chefEquipe)))
                .andExpect(status().isForbidden());
    }

    // --- Filtres, tri, pagination ---

    @Test
    void filtre_par_role_et_par_etat() throws Exception {
        mockMvc.perform(get(ADRESSE).param("role", "AGRICULTEUR").param("statut", "ACTIF")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.elements[0].prenom", is("Ali")));
    }

    @Test
    void filtre_par_delegation() throws Exception {
        mockMvc.perform(get(ADRESSE).param("delegation", "EL_HENCHA").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].nom", contains("Jlassi")));
    }

    @Test
    void la_recherche_trouve_un_nom_sans_tenir_compte_des_majuscules() throws Exception {
        mockMvc.perform(get(ADRESSE).param("texte", "ben sal").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].prenom", contains("Ali")));
    }

    @Test
    void la_recherche_trouve_un_telephone_ecrit_avec_des_espaces() throws Exception {
        mockMvc.perform(get(ADRESSE).param("texte", "98 123").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].prenom", contains("Ali")));
    }

    @Test
    void la_recherche_trouve_une_carte_d_identite() throws Exception {
        mockMvc.perform(get(ADRESSE).param("texte", "0765").header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].prenom", contains("Hamza")));
    }

    @Test
    void tri_par_nom_croissant_puis_decroissant() throws Exception {
        mockMvc.perform(get(ADRESSE).param("role", "AGRICULTEUR").param("tri", "NOM").param("sens", "ASC")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].nom", contains("Ben Salah", "Jlassi", "Kammoun")));

        mockMvc.perform(get(ADRESSE).param("role", "AGRICULTEUR").param("tri", "NOM").param("sens", "DESC")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].nom", contains("Kammoun", "Jlassi", "Ben Salah")));
    }

    @Test
    void le_tri_par_nom_suit_l_ordre_alphabetique_francais_sans_tenir_compte_des_majuscules_ni_des_accents()
            throws Exception {
        creerAgriculteur("Majd", "chorfi", "+21622000001", "22000001", Delegation.THYNA, StatutCompte.ACTIF);
        creerAgriculteur("Emna", "Élouze", "+21622000002", "22000002", Delegation.THYNA, StatutCompte.ACTIF);

        mockMvc.perform(get(ADRESSE).param("role", "AGRICULTEUR").param("tri", "NOM").param("sens", "ASC")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].nom", contains("Ben Salah", "chorfi", "Élouze", "Jlassi", "Kammoun")));
    }

    @Test
    void pagination_renvoie_la_page_demandee_et_le_total() throws Exception {
        mockMvc.perform(get(ADRESSE).param("taille", "4").param("page", "1").param("tri", "NOM").param("sens", "ASC")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.elements", hasSize(2)));
    }

    @Test
    void la_liste_ne_contient_jamais_de_mot_de_passe() throws Exception {
        mockMvc.perform(get(ADRESSE).header("Authorization", jetonAdministrateur))
                .andExpect(jsonPath("$.elements[*].motDePasse").isEmpty())
                .andExpect(jsonPath("$.elements[*].role", everyItem(notNullValue())));
    }

    // --- Actions ---

    @Test
    void creer_un_compte_du_personnel_donne_un_mot_de_passe_provisoire_qui_permet_de_se_connecter() throws Exception {
        String reponse = mockMvc.perform(post(ADRESSE).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compte("Rim", "Gharbi", "98 200 003", "RESPONSABLE_LOGISTIQUE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compte.motDePasseProvisoire", is(true)))
                .andReturn().getResponse().getContentAsString();
        String motDePasseProvisoire = JsonPath.read(reponse, "$.motDePasseProvisoire");

        String jeton = jetonDe("98200003", motDePasseProvisoire, false);
        mockMvc.perform(get("/api/profil").header("Authorization", jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("RESPONSABLE_LOGISTIQUE")));
    }

    @Test
    void l_administrateur_ne_cree_pas_de_compte_agriculteur() throws Exception {
        mockMvc.perform(post(ADRESSE).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compte("Test", "Agriculteur", "98 200 009", "AGRICULTEUR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("ROLE_NON_AUTORISE")));
    }

    @Test
    void un_telephone_deja_utilise_est_refuse_a_la_creation() throws Exception {
        mockMvc.perform(post(ADRESSE).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compte("Autre", "Personne", "98 200 002", "CHEF_EQUIPE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champs.telephone").exists());
    }

    @Test
    void desactiver_un_compte_coupe_son_acces_immediatement() throws Exception {
        String jetonChef = jetonDe(chefEquipe);
        mockMvc.perform(get("/api/profil").header("Authorization", jetonChef)).andExpect(status().isOk());

        mockMvc.perform(post(ADRESSE + "/" + chefEquipe.getId() + "/desactiver").header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("DESACTIVE")));

        // Le jeton du chef d'équipe est encore valide en date, mais le compte n'est plus actif
        mockMvc.perform(get("/api/profil").header("Authorization", jetonChef)).andExpect(status().isUnauthorized());
    }

    @Test
    void un_compte_desactive_peut_etre_reactive() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + chefEquipe.getId() + "/desactiver").header("Authorization", jetonAdministrateur));

        mockMvc.perform(post(ADRESSE + "/" + chefEquipe.getId() + "/reactiver").header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("ACTIF")));
    }

    @Test
    void l_administrateur_ne_peut_pas_se_desactiver_lui_meme() throws Exception {
        mockMvc.perform(post(ADRESSE + "/" + administrateur.getId() + "/desactiver").header("Authorization", jetonAdministrateur))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ACTION_SUR_SOI_INTERDITE")));
    }

    @Test
    void un_agriculteur_ne_peut_pas_devenir_membre_du_personnel() throws Exception {
        Utilisateur ali = utilisateurRepository.findByTelephone("+21698123412").orElseThrow();

        mockMvc.perform(put(ADRESSE + "/" + ali.getId()).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compte("Ali", "Ben Salah", "98123412", "CHEF_EQUIPE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CHANGEMENT_ROLE_INTERDIT")));
    }

    @Test
    void modifier_un_compte_change_ses_informations_et_son_role() throws Exception {
        mockMvc.perform(put(ADRESSE + "/" + chefEquipe.getId()).header("Authorization", jetonAdministrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compte("Karim", "Trabelsi", "98200002", "RESPONSABLE_COLLECTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("RESPONSABLE_COLLECTE")));
    }

    @Test
    void reinitialiser_le_mot_de_passe_remplace_l_ancien() throws Exception {
        String reponse = mockMvc.perform(post(ADRESSE + "/" + chefEquipe.getId() + "/mot-de-passe")
                        .header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nouveauMotDePasse = JsonPath.read(reponse, "$.motDePasseProvisoire");

        mockMvc.perform(post("/api/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"+21698200002\",\"motDePasse\":\"" + MOT_DE_PASSE + "\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(jetonDe("+21698200002", nouveauMotDePasse, false)).startsWith("Bearer ");
    }

    @Test
    void les_statistiques_comptent_les_comptes_par_etat_et_par_role() throws Exception {
        mockMvc.perform(get("/api/administration/statistiques").header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscriptionsEnAttente", is(1)))
                .andExpect(jsonPath("$.comptesActifs", is(4)))
                .andExpect(jsonPath("$.comptesDesactives", is(1)))
                .andExpect(jsonPath("$.totalComptes", is(6)));
    }

    private static String compte(String prenom, String nom, String telephone, String role) {
        return """
                {"prenom":"%s","nom":"%s","telephone":"%s","email":null,"role":"%s"}
                """.formatted(prenom, nom, telephone, role);
    }
}
