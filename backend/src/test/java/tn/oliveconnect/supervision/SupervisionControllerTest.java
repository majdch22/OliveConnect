package tn.oliveconnect.supervision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;

/** Supervision : état des composants, mesures, sauvegarde réelle avec mongodump. */
class SupervisionControllerTest extends TestAvecComptes {

    @Autowired
    private Echantillonneur echantillonneur;

    @Autowired
    private SauvegardeService sauvegardeService;

    private String jetonAdministrateur;

    @BeforeEach
    void preparer() throws Exception {
        jetonAdministrateur = jetonDe(creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF));
    }

    @Test
    void l_etat_montre_la_base_de_donnees_operationnelle_et_l_utilisateur_actif() throws Exception {
        echantillonneur.releverMesures();

        mockMvc.perform(get("/api/administration/supervision").header("Authorization", jetonAdministrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.composants[?(@.nom == 'Base de données MongoDB')].etat", hasItem("OPERATIONNEL")))
                .andExpect(jsonPath("$.composants[?(@.nom == 'Base de données MongoDB')].detail",
                        hasItem(startsWith("MongoDB "))))
                // Au moins l'administrateur du test (les autres tests de la même exécution peuvent aussi être comptés)
                .andExpect(jsonPath("$.indicateurs.utilisateursActifs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.historique").isNotEmpty());
    }

    @Test
    void la_supervision_est_refusee_aux_autres_roles() throws Exception {
        String jetonChef = jetonDe(creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF));

        mockMvc.perform(get("/api/administration/supervision").header("Authorization", jetonChef))
                .andExpect(status().isForbidden());
    }

    @Test
    void une_sauvegarde_cree_un_fichier_compresse_et_apparait_dans_la_liste() throws Exception {
        mockMvc.perform(post("/api/administration/sauvegardes").header("Authorization", jetonAdministrateur))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom", startsWith("oliveconnect-")));

        assertThat(sauvegardeService.lister()).isNotEmpty();
        assertThat(sauvegardeService.lister().getFirst().tailleOctets()).isPositive();
    }

    @Test
    void les_durees_sont_ecrites_en_toutes_lettres() {
        assertThat(SupervisionService.dureeLisible(Duration.ofMinutes(12))).isEqualTo("12 min");
        assertThat(SupervisionService.dureeLisible(Duration.ofMinutes(135))).isEqualTo("2 h 15 min");
        assertThat(SupervisionService.dureeLisible(Duration.ofHours(76))).isEqualTo("3 j 4 h");
        assertThat(SupervisionService.dureeLisible(Duration.ofSeconds(20))).isEqualTo("moins d'une minute");
    }

    @Test
    void les_tailles_sont_ecrites_en_ko_ou_en_mo() {
        assertThat(SupervisionService.tailleLisible(300_000)).isEqualTo("293 Ko");
        assertThat(SupervisionService.tailleLisible(5_242_880)).isEqualTo("5 Mo");
    }
}
