package tn.oliveconnect.agriculteur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandeRecolteRepository;
import tn.oliveconnect.parametres.FrequenceImport;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresRepository;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.referentiel.Variete;
import tn.oliveconnect.referentiel.VarieteRepository;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.verger.Verger;

/**
 * Espace de l'agriculteur : vergers, demandes de récolte et signalements.
 * Chaque agriculteur ne voit et ne modifie que ses propres données.
 */
class EspaceAgriculteurTest extends TestAvecComptes {

    private static final LocalDate AUJOURD_HUI = LocalDate.now(ZoneId.of("Africa/Tunis"));

    @Autowired
    private VarieteRepository varieteRepository;

    @Autowired
    private ParametresRepository parametresRepository;

    @Autowired
    private ParametresService parametresService;

    @Autowired
    private DemandeRecolteRepository demandeRepository;

    private String jetonAli;
    private String jetonHela;
    private String varieteId;

    @BeforeEach
    void preparer() throws Exception {
        mongoTemplate.remove(new Query(), Verger.class);
        mongoTemplate.remove(new Query(), DemandeRecolte.class);
        mongoTemplate.remove(new Query(), Alerte.class);
        campagneAutourDAujourdhui();

        Variete chemlali = new Variete();
        chemlali.setCode("VAR-01");
        chemlali.setNom("Chemlali");
        chemlali.setRendementMoyenKgParHectare(3000);
        chemlali.setMoisDebutRecolte(11);
        chemlali.setMoisFinRecolte(2);
        varieteId = varieteRepository.save(chemlali).getId();

        jetonAli = jetonDe(creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF));
        jetonHela = jetonDe(creerAgriculteur("Hela", "Frikha", "+21697111333", "03456789", Delegation.AGAREB, StatutCompte.ACTIF));
    }

    // --- Vergers ---

    @Test
    void un_agriculteur_ajoute_un_verger_qui_recoit_un_code() throws Exception {
        creerVerger(jetonAli, "Verger Nord")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("V-0001")))
                .andExpect(jsonPath("$.nomVariete", is("Chemlali")))
                .andExpect(jsonPath("$.statutRecolte", is("NON_MUR")));
    }

    @Test
    void un_agriculteur_ne_voit_ni_ne_modifie_les_vergers_d_un_autre() throws Exception {
        String vergerAli = id(creerVerger(jetonAli, "Verger Nord"));

        mockMvc.perform(get("/api/agriculteur/vergers").header("Authorization", jetonHela))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(put("/api/agriculteur/vergers/" + vergerAli).header("Authorization", jetonHela)
                        .contentType(MediaType.APPLICATION_JSON).content(verger("Volé", 34.81, 10.77)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("VERGER_D_UN_AUTRE_AGRICULTEUR")));
    }

    @Test
    void un_verger_hors_de_la_region_de_sfax_est_refuse() throws Exception {
        mockMvc.perform(post("/api/agriculteur/vergers").header("Authorization", jetonAli)
                        .contentType(MediaType.APPLICATION_JSON).content(verger("Verger de Tunis", 36.80, 10.18)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.latitude").exists());
    }

    @Test
    void l_estimation_multiplie_la_superficie_par_le_rendement_de_la_variete() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));

        // 4,5 ha × 3 000 kg/ha = 13 500 kg
        mockMvc.perform(get("/api/agriculteur/vergers/" + vergerId + "/estimation").header("Authorization", jetonAli))
                .andExpect(jsonPath("$.quantiteKg", is(13500)));
    }

    // --- Demandes de récolte ---

    @Test
    void declarer_une_recolte_planifiee_pour_demain_cree_une_demande_soumise() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));

        declarer(jetonAli, vergerId, "PLANIFIEE", AUJOURD_HUI.plusDays(1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", is("D-0001")))
                .andExpect(jsonPath("$.statut", is("SOUMISE")))
                .andExpect(jsonPath("$.modifiable", is(true)))
                .andExpect(jsonPath("$.historique", hasSize(1)));
    }

    @Test
    void une_recolte_planifiee_ne_peut_pas_etre_pour_aujourd_hui_mais_une_urgente_oui() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));

        declarer(jetonAli, vergerId, "PLANIFIEE", AUJOURD_HUI)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.dateSouhaitee", containsString("demain")));
        declarer(jetonAli, vergerId, "URGENTE", AUJOURD_HUI).andExpect(status().isCreated());
    }

    @Test
    void une_date_hors_de_la_campagne_est_refusee() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));

        declarer(jetonAli, vergerId, "PLANIFIEE", AUJOURD_HUI.plusDays(200))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.dateSouhaitee", containsString("campagne")));
    }

    @Test
    void deux_demandes_actives_pour_le_meme_verger_et_la_meme_date_sont_refusees() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));
        declarer(jetonAli, vergerId, "PLANIFIEE", AUJOURD_HUI.plusDays(2));

        declarer(jetonAli, vergerId, "URGENTE", AUJOURD_HUI.plusDays(2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEMANDE_EN_DOUBLE")));
    }

    @Test
    void declarer_une_recolte_sur_le_verger_d_un_autre_est_refuse() throws Exception {
        String vergerAli = id(creerVerger(jetonAli, "Verger Nord"));

        declarer(jetonHela, vergerAli, "PLANIFIEE", AUJOURD_HUI.plusDays(1)).andExpect(status().isForbidden());
    }

    @Test
    void une_demande_annulee_ne_peut_plus_etre_modifiee() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));
        String demandeId = id(declarer(jetonAli, vergerId, "PLANIFIEE", AUJOURD_HUI.plusDays(1)));

        mockMvc.perform(post("/api/agriculteur/demandes/" + demandeId + "/annuler").header("Authorization", jetonAli)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Olives pas encore mûres\"}"))
                .andExpect(jsonPath("$.statut", is("ANNULEE")))
                .andExpect(jsonPath("$.historique", hasSize(2)));

        mockMvc.perform(put("/api/agriculteur/demandes/" + demandeId).header("Authorization", jetonAli)
                        .contentType(MediaType.APPLICATION_JSON).content(demande(vergerId, "PLANIFIEE", AUJOURD_HUI.plusDays(3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEMANDE_NON_MODIFIABLE")));
    }

    @Test
    void deux_modifications_simultanees_d_une_demande_sont_detectees() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));
        String demandeId = id(declarer(jetonAli, vergerId, "PLANIFIEE", AUJOURD_HUI.plusDays(1)));

        // Deux personnes ouvrent la même demande…
        DemandeRecolte ouverteParLaPremiere = demandeRepository.findById(demandeId).orElseThrow();
        DemandeRecolte ouverteParLaSeconde = demandeRepository.findById(demandeId).orElseThrow();
        // … la première enregistre, puis la seconde essaie d'enregistrer une version périmée
        ouverteParLaPremiere.setQuantiteEstimeeKg(4000);
        demandeRepository.save(ouverteParLaPremiere);
        ouverteParLaSeconde.setQuantiteEstimeeKg(1000);

        assertThatThrownBy(() -> demandeRepository.save(ouverteParLaSeconde))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(demandeRepository.findById(demandeId).orElseThrow().getQuantiteEstimeeKg()).isEqualTo(4000);
    }

    // --- Signalements ---

    @Test
    void un_signalement_pres_du_verger_n_est_pas_marque_loin() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));

        signaler(jetonAli, vergerId, 34.8102, 10.7702)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", is("AL-0001")))
                .andExpect(jsonPath("$.statut", is("OUVERTE")))
                .andExpect(jsonPath("$.loinDuVerger", is(false)));
    }

    @Test
    void un_signalement_a_plusieurs_kilometres_du_verger_est_marque_loin() throws Exception {
        String vergerId = id(creerVerger(jetonAli, "Verger Nord"));

        signaler(jetonAli, vergerId, 34.86, 10.77)
                .andExpect(jsonPath("$.loinDuVerger", is(true)))
                .andExpect(jsonPath("$.distanceAuVergerMetres", is(5560)));
    }

    @Test
    void l_espace_agriculteur_est_refuse_au_personnel_avec_un_message_clair() throws Exception {
        String jetonChef = jetonDe(creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF));

        mockMvc.perform(get("/api/agriculteur/vergers").header("Authorization", jetonChef))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCES_REFUSE")));
    }

    // --- Outils des tests ---

    /** Campagne de récolte de 30 jours avant à 90 jours après aujourd'hui, pour que les tests marchent toute l'année. */
    private void campagneAutourDAujourdhui() {
        parametresRepository.save(new Parametres(Parametres.IDENTIFIANT,
                new Parametres.Campagne(AUJOURD_HUI.minusDays(30), AUJOURD_HUI.plusDays(90)),
                new Parametres.ReglesMetier(70, 110, 500, 24),
                new Parametres.Imports(FrequenceImport.CHAQUE_JOUR, FrequenceImport.TOUTES_LES_15_MINUTES,
                        FrequenceImport.TOUTES_LES_15_MINUTES),
                new Parametres.Notifications(true, false),
                new Parametres.Securite(8, 5, 15),
                Instant.now(), "test"));
        parametresService.relireDepuisLaBase();
    }

    private ResultActions creerVerger(String jeton, String nom) throws Exception {
        return mockMvc.perform(post("/api/agriculteur/vergers").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(verger(nom, 34.81, 10.77)));
    }

    private String verger(String nom, double latitude, double longitude) {
        return """
                {"nom":"%s","latitude":%s,"longitude":%s,"delegation":"SAKIET_EZZIT","superficieHa":4.5,"nombreArbres":380,"varieteId":"%s"}
                """.formatted(nom, latitude, longitude, varieteId);
    }

    private ResultActions declarer(String jeton, String vergerId, String type, LocalDate date) throws Exception {
        return mockMvc.perform(post("/api/agriculteur/demandes").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(demande(vergerId, type, date)));
    }

    private static String demande(String vergerId, String type, LocalDate date) {
        return """
                {"vergerId":"%s","type":"%s","dateSouhaitee":"%s","quantiteEstimeeKg":3500,"commentaire":null}
                """.formatted(vergerId, type, date);
    }

    private ResultActions signaler(String jeton, String vergerId, double latitude, double longitude) throws Exception {
        return mockMvc.perform(post("/api/agriculteur/signalements").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vergerId":"%s","type":"RAVAGEUR","latitude":%s,"longitude":%s,"description":"Mouche de l'olive"}
                        """.formatted(vergerId, latitude, longitude)));
    }

    private static String id(ResultActions resultat) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
