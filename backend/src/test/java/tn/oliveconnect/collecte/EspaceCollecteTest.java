package tn.oliveconnect.collecte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.notification.Notification;
import tn.oliveconnect.parametres.FrequenceImport;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresRepository;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.referentiel.PressoirRepository;
import tn.oliveconnect.referentiel.Variete;
import tn.oliveconnect.referentiel.VarieteRepository;
import tn.oliveconnect.ressource.Equipe;
import tn.oliveconnect.ressource.EtatRessource;
import tn.oliveconnect.ressource.Ressource;
import tn.oliveconnect.ressource.RessourceRepository;
import tn.oliveconnect.ressource.TypeRessource;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;

/**
 * Espace du responsable de collecte : examen des demandes, planification des tournées (conflits et avertissements),
 * suivi et annulation des tournées, traitement des signalements, carte et tableau de bord.
 */
class EspaceCollecteTest extends TestAvecComptes {

    private static final LocalDate AUJOURD_HUI = LocalDate.now(ZoneId.of("Africa/Tunis"));
    private static final LocalDate DEMAIN = AUJOURD_HUI.plusDays(1);

    @Autowired
    private ParametresRepository parametresRepository;

    @Autowired
    private ParametresService parametresService;

    @Autowired
    private VarieteRepository varieteRepository;

    @Autowired
    private VergerRepository vergerRepository;

    @Autowired
    private PressoirRepository pressoirRepository;

    @Autowired
    private RessourceRepository ressourceRepository;

    private String jetonSalma;
    private String jetonAli;
    private String jetonHela;
    private Utilisateur ali;
    private Utilisateur karim;
    private Utilisateur sami;
    private String varieteId;
    private String pressoirAmra;
    private String pressoirAgareb;
    private String tracteur;
    private String benneDeuxTonnes;
    private String benneTroisTonnes;

    @BeforeEach
    void preparer() throws Exception {
        for (Class<?> collection : List.of(Verger.class, DemandeRecolte.class, Alerte.class, Tournee.class, Ressource.class,
                Equipe.class)) {
            mongoTemplate.remove(new Query(), collection);
        }
        campagneAutourDAujourdhui();

        Variete chemlali = new Variete();
        chemlali.setCode("VAR-01");
        chemlali.setNom("Chemlali");
        chemlali.setRendementMoyenKgParHectare(3000);
        chemlali.setMoisDebutRecolte(11);
        chemlali.setMoisFinRecolte(2);
        varieteId = varieteRepository.save(chemlali).getId();

        pressoirAmra = pressoir("PR-01", "Huilerie El Amra", 34.80, 10.75, 5000);
        pressoirAgareb = pressoir("PR-02", "Pressoir d'Agareb", 34.74, 10.53, 20000);
        tracteur = ressource("TR-01", TypeRessource.TRACTEUR, null, EtatRessource.DISPONIBLE);
        benneDeuxTonnes = ressource("BN-01", TypeRessource.BENNE, 2000, EtatRessource.DISPONIBLE);
        benneTroisTonnes = ressource("BN-02", TypeRessource.BENNE, 3000, EtatRessource.DISPONIBLE);

        Utilisateur salma = creerPersonnel("Salma", "Chaabane", "+21698200001", Role.RESPONSABLE_COLLECTE, StatutCompte.ACTIF);
        karim = creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        sami = creerPersonnel("Sami", "Bouzid", "+21698200005", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        ali = creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);
        Utilisateur hela = creerAgriculteur("Hela", "Frikha", "+21697111333", "03456789", Delegation.AGAREB, StatutCompte.ACTIF);
        jetonSalma = jetonDe(salma);
        jetonAli = jetonDe(ali);
        jetonHela = jetonDe(hela);
    }

    // --- Droits ---

    @Test
    void un_agriculteur_n_accede_pas_a_l_espace_de_collecte() throws Exception {
        mockMvc.perform(get("/api/collecte/demandes").header("Authorization", jetonAli))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/collecte/tournees").header("Authorization", jetonAli))
                .andExpect(status().isForbidden());
    }

    // --- Examen des demandes ---

    @Test
    void la_file_presente_les_urgentes_d_abord_puis_les_vergers_les_plus_murs() throws Exception {
        String vergerPeuMur = verger(jetonAli, "Verger peu mûr", 40);
        String vergerTresMur = verger(jetonAli, "Verger très mûr", 85);
        String vergerUrgent = verger(jetonHela, "Verger urgent", 10);
        declarer(jetonAli, vergerPeuMur, "PLANIFIEE", DEMAIN);
        declarer(jetonAli, vergerTresMur, "PLANIFIEE", DEMAIN.plusDays(1));
        declarer(jetonHela, vergerUrgent, "URGENTE", AUJOURD_HUI);

        mockMvc.perform(get("/api/collecte/demandes?statut=SOUMISE").header("Authorization", jetonSalma))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.elements[0].nomVerger", is("Verger urgent")))
                .andExpect(jsonPath("$.elements[1].nomVerger", is("Verger très mûr")))
                .andExpect(jsonPath("$.elements[1].niveauMaturite", is(85)))
                .andExpect(jsonPath("$.elements[1].nomAgriculteur", is("Ali Ben Salah")))
                .andExpect(jsonPath("$.elements[2].nomVerger", is("Verger peu mûr")));
        mockMvc.perform(get("/api/collecte/demandes/compteurs").header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.SOUMISE", is(3)))
                .andExpect(jsonPath("$.URGENTES_A_TRAITER", is(1)));
    }

    @Test
    void valider_une_demande_previent_l_agriculteur_et_ne_se_fait_qu_une_fois() throws Exception {
        String demande = demandeSoumise(jetonAli, "Verger Nord", DEMAIN);

        mockMvc.perform(post("/api/collecte/demandes/" + demande + "/valider").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VALIDEE")))
                .andExpect(jsonPath("$.historique[1].nomAuteur", is("Salma Chaabane")));
        assertThat(titresNotificationsDe(ali)).anyMatch(titre -> titre.contains("validée"));

        mockMvc.perform(post("/api/collecte/demandes/" + demande + "/valider").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEMANDE_DEJA_TRAITEE")));
    }

    @Test
    void rejeter_une_demande_exige_un_motif_que_l_agriculteur_voit() throws Exception {
        String demande = demandeSoumise(jetonAli, "Verger Nord", DEMAIN);

        mockMvc.perform(post("/api/collecte/demandes/" + demande + "/rejeter").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motif").exists());
        mockMvc.perform(post("/api/collecte/demandes/" + demande + "/rejeter").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Verger non mûr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("REJETEE")));

        mockMvc.perform(get("/api/agriculteur/demandes/" + demande).header("Authorization", jetonAli))
                .andExpect(jsonPath("$.statut", is("REJETEE")))
                .andExpect(jsonPath("$.motifRejet", is("Verger non mûr")));
        assertThat(smsEnvoyesA(ali.getTelephone())).anyMatch(texte -> texte.contains("Verger non mûr"));
    }

    @Test
    void la_fiche_d_une_demande_indique_le_pressoir_le_plus_proche() throws Exception {
        String demande = demandeSoumise(jetonAli, "Verger Nord", DEMAIN);

        mockMvc.perform(get("/api/collecte/demandes/" + demande).header("Authorization", jetonSalma))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demande.numero", is("D-0001")))
                .andExpect(jsonPath("$.pressoirLePlusProche", is("Huilerie El Amra")))
                .andExpect(jsonPath("$.distancePressoirKm", is(2)));
    }

    // --- Planification ---

    @Test
    void planifier_une_tournee_passe_les_demandes_en_planifiee_et_previent_agriculteur_et_chef() throws Exception {
        String demande = demandeValidee(jetonAli, "Verger Nord", DEMAIN);

        String tournee = id(planifier(List.of(demande), karim.getId(), pressoirAmra, List.of(tracteur, benneDeuxTonnes, benneTroisTonnes))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", is("T-0001")))
                .andExpect(jsonPath("$.statut", is("PLANIFIEE")))
                .andExpect(jsonPath("$.nomChefEquipe", is("Karim Trabelsi")))
                .andExpect(jsonPath("$.etapes[0].nomAgriculteur", is("Ali Ben Salah")))
                .andExpect(jsonPath("$.capaciteBennesKg", is(5000))));

        mockMvc.perform(get("/api/agriculteur/demandes/" + demande).header("Authorization", jetonAli))
                .andExpect(jsonPath("$.statut", is("PLANIFIEE")))
                .andExpect(jsonPath("$.numeroTournee", is("T-0001")))
                .andExpect(jsonPath("$.datePlanifiee", is(DEMAIN.toString())));
        assertThat(titresNotificationsDe(ali)).anyMatch(titre -> titre.startsWith("Collecte planifiée"));
        assertThat(titresNotificationsDe(karim)).contains("Nouvelle tournée T-0001");
        mockMvc.perform(get("/api/collecte/tournees?date=" + DEMAIN).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(tournee)));
    }

    @Test
    void une_ressource_deja_affectee_le_meme_jour_est_un_conflit() throws Exception {
        String premiere = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        String seconde = demandeValidee(jetonHela, "Verger d'Agareb", DEMAIN);
        planifier(List.of(premiere), karim.getId(), pressoirAmra, List.of(tracteur, benneDeuxTonnes))
                .andExpect(status().isCreated());

        planifier(List.of(seconde), sami.getId(), pressoirAmra, List.of(tracteur, benneTroisTonnes))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PLANIFICATION_IMPOSSIBLE")))
                .andExpect(jsonPath("$.message", containsString("TR-01 est déjà affecté à la tournée T-0001")));

        // Les options du jour ne proposent plus ce tracteur
        mockMvc.perform(get("/api/collecte/planification/options?date=" + DEMAIN).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.ressourcesOccupees", is(2)))
                .andExpect(jsonPath("$.ressources[*].code", org.hamcrest.Matchers.not(hasItem("TR-01"))));
    }

    @Test
    void un_chef_d_equipe_n_a_qu_une_tournee_par_jour() throws Exception {
        String premiere = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        String seconde = demandeValidee(jetonHela, "Verger d'Agareb", DEMAIN);
        planifier(List.of(premiere), karim.getId(), pressoirAmra, List.of(benneDeuxTonnes)).andExpect(status().isCreated());

        planifier(List.of(seconde), karim.getId(), pressoirAmra, List.of(benneTroisTonnes))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Karim Trabelsi a déjà la tournée T-0001")));
    }

    @Test
    void une_ressource_en_panne_ne_peut_pas_etre_affectee() throws Exception {
        String demande = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        String enPanne = ressource("TR-02", TypeRessource.TRACTEUR, null, EtatRessource.EN_PANNE);

        planifier(List.of(demande), karim.getId(), pressoirAmra, List.of(enPanne))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("TR-02 (Ressource TR-02) est indisponible : en panne")));
    }

    @Test
    void seule_une_demande_validee_peut_etre_planifiee() throws Exception {
        String soumise = demandeSoumise(jetonAli, "Verger Nord", DEMAIN);

        planifier(List.of(soumise), karim.getId(), pressoirAmra, List.of(benneDeuxTonnes))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("D-0001 n'est plus validée")));
    }

    @Test
    void la_verification_avertit_si_les_bennes_ou_le_pressoir_ne_suffisent_pas_et_propose_un_autre_pressoir() throws Exception {
        String demande = demandeValidee(jetonAli, "Verger Nord", DEMAIN, 6000);

        mockMvc.perform(post("/api/collecte/planification/verification").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planification(List.of(demande), karim.getId(), pressoirAmra, List.of(benneDeuxTonnes))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflits", hasSize(0)))
                .andExpect(jsonPath("$.quantiteEstimeeKg", is(6000)))
                .andExpect(jsonPath("$.avertissements", hasItem(containsString("Capacité des bennes 2 000 kg < 6 000 kg"))))
                .andExpect(jsonPath("$.avertissements", hasItem(containsString("Huilerie El Amra n'a plus que 5 000 kg"))))
                .andExpect(jsonPath("$.pressoirsSuggeres[0].nom", is("Pressoir d'Agareb")));
    }

    @Test
    void la_capacite_reservee_d_un_pressoir_compte_les_tournees_du_jour() throws Exception {
        String demande = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        planifier(List.of(demande), karim.getId(), pressoirAmra, List.of(benneTroisTonnes)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/collecte/planification/options?date=" + DEMAIN).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.pressoirs[?(@.code == 'PR-01')].reserveKg", hasItem(3500)))
                .andExpect(jsonPath("$.pressoirs[?(@.code == 'PR-01')].restantKg", hasItem(1500)))
                .andExpect(jsonPath("$.chefs[*].nomComplet", org.hamcrest.Matchers.not(hasItem("Karim Trabelsi"))));
    }

    @Test
    void une_tournee_ne_se_planifie_pas_dans_le_passe() throws Exception {
        String demande = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        mockMvc.perform(post("/api/collecte/tournees").header("Authorization", jetonSalma).contentType(MediaType.APPLICATION_JSON)
                        .content(planification(List.of(demande), karim.getId(), pressoirAmra, List.of(), AUJOURD_HUI.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.date").exists());
    }

    // --- Suivi des tournées ---

    @Test
    void annuler_une_tournee_remet_ses_demandes_a_planifier() throws Exception {
        String demande = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        String tournee = id(planifier(List.of(demande), karim.getId(), pressoirAmra, List.of(benneTroisTonnes)));

        mockMvc.perform(post("/api/collecte/tournees/" + tournee + "/annuler").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/collecte/tournees/" + tournee + "/annuler").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Pluie annoncée\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("ANNULEE")))
                .andExpect(jsonPath("$.annulable", is(false)));

        mockMvc.perform(get("/api/agriculteur/demandes/" + demande).header("Authorization", jetonAli))
                .andExpect(jsonPath("$.statut", is("VALIDEE")))
                .andExpect(jsonPath("$.numeroTournee", nullValue()));
        assertThat(titresNotificationsDe(karim)).contains("Tournée T-0001 annulée");
        // La ressource redevient libre ce jour-là
        planifier(List.of(demande), sami.getId(), pressoirAmra, List.of(benneTroisTonnes)).andExpect(status().isCreated());
    }

    @Test
    void l_annulation_d_une_demande_planifiee_par_l_agriculteur_libere_sa_place_dans_la_tournee() throws Exception {
        String demandeAli = demandeValidee(jetonAli, "Verger Nord", DEMAIN);
        String demandeHela = demandeValidee(jetonHela, "Verger d'Agareb", DEMAIN);
        String tournee = id(planifier(List.of(demandeAli, demandeHela), karim.getId(), pressoirAgareb, List.of(benneTroisTonnes)));

        mockMvc.perform(post("/api/agriculteur/demandes/" + demandeAli + "/annuler").header("Authorization", jetonAli)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/collecte/tournees/" + tournee).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.etapes", hasSize(1)))
                .andExpect(jsonPath("$.etapes[0].ordre", is(1)))
                .andExpect(jsonPath("$.etapes[0].nomVerger", is("Verger d'Agareb")))
                .andExpect(jsonPath("$.statut", is("PLANIFIEE")));

        // Le dernier verger retiré : la tournée est annulée d'elle-même
        mockMvc.perform(post("/api/agriculteur/demandes/" + demandeHela + "/annuler").header("Authorization", jetonHela)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/collecte/tournees/" + tournee).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.statut", is("ANNULEE")));
    }

    @Test
    void changer_la_date_d_une_demande_planifiee_la_sort_de_la_tournee() throws Exception {
        String vergerId = verger(jetonAli, "Verger Nord", null);
        String demande = valider(id(declarer(jetonAli, vergerId, "PLANIFIEE", DEMAIN)));
        String tournee = id(planifier(List.of(demande), karim.getId(), pressoirAmra, List.of(benneTroisTonnes)));

        mockMvc.perform(put("/api/agriculteur/demandes/" + demande).header("Authorization", jetonAli)
                        .contentType(MediaType.APPLICATION_JSON).content(demande(vergerId, "PLANIFIEE", DEMAIN.plusDays(3), 3500)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VALIDEE")))
                .andExpect(jsonPath("$.numeroTournee", nullValue()));
        mockMvc.perform(get("/api/collecte/tournees/" + tournee).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.statut", is("ANNULEE")));
    }

    // --- Signalements ---

    @Test
    void un_signalement_se_prend_en_charge_puis_se_resout_avec_une_reponse_pour_l_agriculteur() throws Exception {
        String vergerId = verger(jetonAli, "Verger Nord", null);
        String alerte = id(signaler(jetonAli, vergerId));

        mockMvc.perform(post("/api/collecte/alertes/" + alerte + "/prendre-en-charge").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commentaire\":\"Équipe de traitement demain matin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("PRISE_EN_CHARGE")))
                .andExpect(jsonPath("$.traitements[0].nomAuteur", is("Salma Chaabane")));
        mockMvc.perform(post("/api/collecte/alertes/" + alerte + "/resoudre").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commentaire\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/collecte/alertes/" + alerte + "/resoudre").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commentaire\":\"Traitement fait sur 30 arbres\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("RESOLUE")));

        mockMvc.perform(get("/api/agriculteur/signalements").header("Authorization", jetonAli))
                .andExpect(jsonPath("$[0].statut", is("RESOLUE")))
                .andExpect(jsonPath("$[0].reponseCooperative", is("Traitement fait sur 30 arbres")));
        assertThat(titresNotificationsDe(ali)).anyMatch(titre -> titre.endsWith("résolu"));
        mockMvc.perform(get("/api/collecte/tableau-de-bord").header("Authorization", jetonSalma))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertesParStatut.RESOLUE", is(1)))
                .andExpect(jsonPath("$.delaiMoyenPriseEnChargeMinutes", is(0)));
    }

    @Test
    void une_demande_urgente_peut_etre_creee_depuis_un_signalement() throws Exception {
        String vergerId = verger(jetonAli, "Verger Nord", 90);
        String alerte = id(signaler(jetonAli, vergerId));

        mockMvc.perform(post("/api/collecte/alertes/" + alerte + "/demande-urgente").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\",\"quantiteEstimeeKg\":2500,\"commentaire\":null}".formatted(AUJOURD_HUI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("PRISE_EN_CHARGE")))
                .andExpect(jsonPath("$.numeroDemandeUrgente", is("D-0001")));

        mockMvc.perform(get("/api/agriculteur/demandes").header("Authorization", jetonAli))
                .andExpect(jsonPath("$[0].type", is("URGENTE")))
                .andExpect(jsonPath("$[0].statut", is("VALIDEE")));
        mockMvc.perform(post("/api/collecte/alertes/" + alerte + "/demande-urgente").header("Authorization", jetonSalma)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\",\"quantiteEstimeeKg\":2500}".formatted(AUJOURD_HUI)))
                .andExpect(status().isConflict());
    }

    // --- Carte ---

    @Test
    void la_carte_ne_renvoie_que_les_vergers_de_la_zone_visible() throws Exception {
        verger(jetonAli, "Verger Nord", 75);
        mockMvc.perform(get("/api/collecte/carte").header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.vergers", hasSize(1)))
                .andExpect(jsonPath("$.vergers[0].nomAgriculteur", is("Ali Ben Salah")))
                .andExpect(jsonPath("$.pressoirs", hasSize(2)));
        mockMvc.perform(get("/api/collecte/carte?ouest=10.0&sud=34.0&est=10.5&nord=34.5").header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.vergers", hasSize(0)));
    }

    // --- Outils ---

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

    private String pressoir(String code, String nom, double latitude, double longitude, int capacite) {
        Pressoir pressoir = new Pressoir();
        pressoir.setCode(code);
        pressoir.setNom(nom);
        pressoir.setDelegation(Delegation.SAKIET_EZZIT);
        pressoir.setLocalisation(new GeoJsonPoint(longitude, latitude));
        pressoir.setCapaciteJournaliereKg(capacite);
        return pressoirRepository.save(pressoir).getId();
    }

    private String ressource(String code, TypeRessource type, Integer capacite, EtatRessource etat) {
        Ressource ressource = new Ressource();
        ressource.setCode(code);
        ressource.setType(type);
        ressource.setNom("Ressource " + code);
        ressource.setCapaciteKg(capacite);
        ressource.setEtat(etat);
        return ressourceRepository.save(ressource).getId();
    }

    /** Verger d'un agriculteur ; la maturité (facultative) est donnée comme si les capteurs l'avaient mesurée. */
    private String verger(String jeton, String nom, Integer maturite) throws Exception {
        String id = id(mockMvc.perform(post("/api/agriculteur/vergers").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"%s","latitude":34.81,"longitude":10.77,"delegation":"SAKIET_EZZIT","superficieHa":4.5,"nombreArbres":380,"varieteId":"%s"}
                        """.formatted(nom, varieteId))));
        if (maturite != null) {
            Verger verger = vergerRepository.findById(id).orElseThrow();
            verger.setNiveauMaturite(maturite);
            vergerRepository.save(verger);
        }
        return id;
    }

    private ResultActions declarer(String jeton, String vergerId, String type, LocalDate date) throws Exception {
        return mockMvc.perform(post("/api/agriculteur/demandes").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(demande(vergerId, type, date, 3500)));
    }

    private static String demande(String vergerId, String type, LocalDate date, int quantite) {
        return """
                {"vergerId":"%s","type":"%s","dateSouhaitee":"%s","quantiteEstimeeKg":%d,"commentaire":null}
                """.formatted(vergerId, type, date, quantite);
    }

    private String demandeSoumise(String jeton, String nomVerger, LocalDate date) throws Exception {
        return id(declarer(jeton, verger(jeton, nomVerger, null), "PLANIFIEE", date));
    }

    private String demandeValidee(String jeton, String nomVerger, LocalDate date) throws Exception {
        return valider(demandeSoumise(jeton, nomVerger, date));
    }

    private String demandeValidee(String jeton, String nomVerger, LocalDate date, int quantite) throws Exception {
        String vergerId = verger(jeton, nomVerger, null);
        return valider(id(mockMvc.perform(post("/api/agriculteur/demandes").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(demande(vergerId, "PLANIFIEE", date, quantite)))));
    }

    private String valider(String demande) throws Exception {
        mockMvc.perform(post("/api/collecte/demandes/" + demande + "/valider").header("Authorization", jetonSalma)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        return demande;
    }

    private ResultActions planifier(List<String> demandes, String chef, String pressoir, List<String> ressources) throws Exception {
        return mockMvc.perform(post("/api/collecte/tournees").header("Authorization", jetonSalma)
                .contentType(MediaType.APPLICATION_JSON).content(planification(demandes, chef, pressoir, ressources)));
    }

    private static String planification(List<String> demandes, String chef, String pressoir, List<String> ressources) {
        return planification(demandes, chef, pressoir, ressources, DEMAIN);
    }

    private static String planification(List<String> demandes, String chef, String pressoir, List<String> ressources,
            LocalDate date) {
        return """
                {"date":"%s","demandeIds":%s,"chefEquipeId":"%s","pressoirId":"%s","ressourceIds":%s,"commentaire":null}
                """.formatted(date, json(demandes), chef, pressoir, json(ressources));
    }

    private static String json(List<String> valeurs) {
        return valeurs.stream().map(valeur -> "\"" + valeur + "\"").toList().toString();
    }

    private ResultActions signaler(String jeton, String vergerId) throws Exception {
        return mockMvc.perform(post("/api/agriculteur/signalements").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"vergerId":"%s","type":"RAVAGEUR","latitude":34.8101,"longitude":10.7701,"description":"Mouche de l'olive"}
                        """.formatted(vergerId)));
    }

    private List<String> titresNotificationsDe(Utilisateur compte) {
        return mongoTemplate.find(Query.query(org.springframework.data.mongodb.core.query.Criteria.where("destinataireId")
                .is(compte.getId())), Notification.class).stream().map(Notification::getTitre).toList();
    }

    private static String id(ResultActions resultat) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
