package tn.oliveconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Notifications (décision 13) et SMS simulés (décision 17) : chacun ne reçoit et ne lit que les siennes,
 * une notification « pour un rôle » ne touche que les comptes actifs de ce rôle.
 */
class NotificationTest extends TestAvecComptes {

    @Autowired
    private NotificationService notificationService;

    @Test
    void une_notification_pour_un_role_ne_touche_que_les_comptes_actifs_de_ce_role() throws Exception {
        Utilisateur karim = creerPersonnel("Karim", "Trabelsi", "+21650000001", Role.RESPONSABLE_COLLECTE, StatutCompte.ACTIF);
        Utilisateur sonia = creerPersonnel("Sonia", "Gharbi", "+21650000002", Role.RESPONSABLE_COLLECTE, StatutCompte.DESACTIVE);
        Utilisateur logistique = creerPersonnel("Nabil", "Kammoun", "+21650000003", Role.RESPONSABLE_LOGISTIQUE, StatutCompte.ACTIF);

        notificationService.notifierRole(Role.RESPONSABLE_COLLECTE,
                NouvelleNotification.simple(CategorieNotification.COLLECTE, "Demande urgente D-0001", "Verger Nord", null).enPriorite());

        assertThat(notificationService.nombreNonLues(karim.getId())).isEqualTo(1);
        assertThat(notificationService.nombreNonLues(sonia.getId())).isZero();
        assertThat(notificationService.nombreNonLues(logistique.getId())).isZero();
        mockMvc.perform(get("/api/notifications").header("Authorization", jetonDe(karim)))
                .andExpect(jsonPath("$.elements[0].titre", is("Demande urgente D-0001")))
                .andExpect(jsonPath("$.elements[0].prioritaire", is(true)));
    }

    @Test
    void chacun_ne_lit_que_ses_notifications_et_tout_marquer_lu_remet_le_compteur_a_zero() throws Exception {
        Utilisateur ali = creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);
        Utilisateur hela = creerAgriculteur("Hela", "Frikha", "+21697111333", "03456789", Delegation.AGAREB, StatutCompte.ACTIF);
        notificationService.notifierPersonne(ali.getId(), NouvelleNotification.simple(CategorieNotification.COLLECTE, "Tournée planifiée", "02/12", null));
        notificationService.notifierPersonne(ali.getId(), NouvelleNotification.simple(CategorieNotification.ALERTE, "Alerte prise en charge", "Ravageur", null));
        String jetonAli = jetonDe(ali);
        String jetonHela = jetonDe(hela);

        String liste = mockMvc.perform(get("/api/notifications").header("Authorization", jetonAli))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andReturn().getResponse().getContentAsString();
        String notificationAli = JsonPath.read(liste, "$.elements[0].id");

        mockMvc.perform(get("/api/notifications").header("Authorization", jetonHela)).andExpect(jsonPath("$.elements", hasSize(0)));
        mockMvc.perform(post("/api/notifications/" + notificationAli + "/lue").header("Authorization", jetonHela))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notifications").param("categorie", "ALERTE").header("Authorization", jetonAli))
                .andExpect(jsonPath("$.totalElements", is(1)));

        mockMvc.perform(post("/api/notifications/tout-lire").header("Authorization", jetonAli)).andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/nombre-non-lues").header("Authorization", jetonAli))
                .andExpect(jsonPath("$.nombre", is(0)));
    }

    @Test
    void une_personne_qui_refuse_les_notifications_dans_l_application_n_en_recoit_plus() throws Exception {
        Utilisateur ali = creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);
        mockMvc.perform(put("/api/profil/preferences").header("Authorization", jetonDe(ali))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"application\":false,\"sms\":true}"))
                .andExpect(jsonPath("$.preferences.application", is(false)));

        notificationService.notifierPersonne(ali.getId(), NouvelleNotification.simple(CategorieNotification.COLLECTE, "Tournée planifiée", "02/12", null));

        assertThat(notificationService.nombreNonLues(ali.getId())).isZero();
    }

    @Test
    void une_inscription_acceptee_envoie_un_sms_avec_le_numero_d_adherent_et_un_message_de_bienvenue() throws Exception {
        Utilisateur administrateur = creerPersonnel("Admin", "Principal", "+21670000009", Role.ADMINISTRATEUR, StatutCompte.ACTIF);
        Utilisateur ali = creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT,
                StatutCompte.EN_ATTENTE_VALIDATION);

        mockMvc.perform(post("/api/administration/inscriptions/" + ali.getId() + "/accepter")
                        .header("Authorization", jetonDe(administrateur)))
                .andExpect(status().isOk());

        assertThat(smsEnvoyesA("+21698123412")).singleElement().asString().contains("ADH-0001");
        assertThat(notificationService.nombreNonLues(ali.getId())).isEqualTo(1);
    }

    @Test
    void seul_l_administrateur_consulte_les_sms_envoyes() throws Exception {
        Utilisateur administrateur = creerPersonnel("Admin", "Principal", "+21670000009", Role.ADMINISTRATEUR, StatutCompte.ACTIF);
        Utilisateur ali = creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);

        mockMvc.perform(get("/api/administration/messages-envoyes").header("Authorization", jetonDe(ali)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/administration/messages-envoyes").header("Authorization", jetonDe(administrateur)))
                .andExpect(status().isOk());
    }
}
