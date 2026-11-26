package tn.oliveconnect.carte;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/** Carte : agriculteurs comptés par délégation et par état (agrégation MongoDB). */
class CarteControllerTest extends TestAvecComptes {

    @Test
    void les_agriculteurs_sont_comptes_par_delegation_et_par_etat() throws Exception {
        String jeton = jetonDe(creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF));
        creerAgriculteur("Ali", "Ben Salah", "+21698000001", "10000001", Delegation.AGAREB, StatutCompte.ACTIF);
        creerAgriculteur("Hela", "Frikha", "+21698000002", "10000002", Delegation.AGAREB, StatutCompte.ACTIF);
        creerAgriculteur("Nadia", "Kammoun", "+21698000003", "10000003", Delegation.AGAREB, StatutCompte.EN_ATTENTE_VALIDATION);
        creerAgriculteur("Omar", "Fourati", "+21698000004", "10000004", Delegation.EL_HENCHA, StatutCompte.ACTIF);

        mockMvc.perform(get("/api/administration/carte").header("Authorization", jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delegations", hasSize(Delegation.values().length)))
                .andExpect(jsonPath("$.delegations[?(@.code == 'AGAREB')].agriculteursActifs", contains(2)))
                .andExpect(jsonPath("$.delegations[?(@.code == 'AGAREB')].inscriptionsEnAttente", contains(1)))
                .andExpect(jsonPath("$.delegations[?(@.code == 'EL_HENCHA')].agriculteursActifs", contains(1)))
                .andExpect(jsonPath("$.delegations[?(@.code == 'THYNA')].agriculteursActifs", contains(0)));
    }
}
