package tn.oliveconnect.pressoir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.data.mongodb.core.query.Criteria;
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
import tn.oliveconnect.tournee.StatutTournee;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.tournee.TourneeRepository;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.verger.Verger;

/**
 * Espace du responsable du pressoir : réception des bennes (pesée, qualité, refus des pesées incohérentes),
 * clôture de la tournée, capacité publiée, historique, export et tableau de bord.
 * Le chef d'équipe n'ayant pas encore son espace, les tests marquent eux-mêmes les tournées « en cours ».
 */
class EspacePressoirTest extends TestAvecComptes {

    private static final LocalDate AUJOURD_HUI = LocalDate.now(ZoneId.of("Africa/Tunis"));

    @Autowired
    private ParametresRepository parametresRepository;

    @Autowired
    private ParametresService parametresService;

    @Autowired
    private VarieteRepository varieteRepository;

    @Autowired
    private PressoirRepository pressoirRepository;

    @Autowired
    private RessourceRepository ressourceRepository;

    @Autowired
    private TourneeRepository tourneeRepository;

    private String jetonFethi;
    private String jetonSalma;
    private String jetonAli;
    private String jetonHela;
    private Utilisateur ali;
    private Utilisateur karim;
    private String varieteId;
    private String elAmra;
    private String agareb;
    private String benneDeuxTonnes;
    private String benneTroisTonnes;

    @BeforeEach
    void preparer() throws Exception {
        for (Class<?> collection : List.of(Verger.class, DemandeRecolte.class, Alerte.class, Tournee.class, Ressource.class,
                Equipe.class, Reception.class, CapacitePressoir.class)) {
            mongoTemplate.remove(new Query(), collection);
        }
        parametresRepository.save(new Parametres(Parametres.IDENTIFIANT,
                new Parametres.Campagne(AUJOURD_HUI.minusDays(30), AUJOURD_HUI.plusDays(90)),
                new Parametres.ReglesMetier(70, 110, 500, 24),
                new Parametres.Imports(FrequenceImport.CHAQUE_JOUR, FrequenceImport.TOUTES_LES_15_MINUTES,
                        FrequenceImport.TOUTES_LES_15_MINUTES),
                new Parametres.Notifications(true, false), new Parametres.Securite(8, 5, 15), Instant.now(), "test"));
        parametresService.relireDepuisLaBase();

        Variete chemlali = new Variete();
        chemlali.setCode("VAR-01");
        chemlali.setNom("Chemlali");
        chemlali.setRendementMoyenKgParHectare(3000);
        chemlali.setMoisDebutRecolte(11);
        chemlali.setMoisFinRecolte(2);
        varieteId = varieteRepository.save(chemlali).getId();

        elAmra = pressoir("PR-01", "Huilerie El Amra", 20000);
        agareb = pressoir("PR-02", "Pressoir d'Agareb", 15000);
        benneDeuxTonnes = ressource("BN-01", 2000);
        benneTroisTonnes = ressource("BN-02", 3000);

        Utilisateur fethi = creerPersonnel("Fethi", "Ayadi", "+21698200004", Role.RESPONSABLE_PRESSOIR, StatutCompte.ACTIF);
        fethi.setPressoirId(elAmra);
        utilisateurRepository.save(fethi);
        jetonFethi = jetonDe(fethi);
        jetonSalma = jetonDe(creerPersonnel("Salma", "Chaabane", "+21698200001", Role.RESPONSABLE_COLLECTE, StatutCompte.ACTIF));
        karim = creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
        ali = creerAgriculteur("Ali", "Ben Salah", "+21698123412", "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);
        jetonAli = jetonDe(ali);
        jetonHela = jetonDe(creerAgriculteur("Hela", "Frikha", "+21697111333", "03456789", Delegation.AGAREB, StatutCompte.ACTIF));
    }

    // --- Droits ---

    @Test
    void un_responsable_sans_pressoir_est_prevenu_et_un_agriculteur_est_refuse() throws Exception {
        Utilisateur sansPressoir = creerPersonnel("Nadia", "Sans", "+21698200009", Role.RESPONSABLE_PRESSOIR, StatutCompte.ACTIF);
        mockMvc.perform(get("/api/pressoir").header("Authorization", jetonDe(sansPressoir)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("AUCUN_PRESSOIR")));
        mockMvc.perform(get("/api/pressoir").header("Authorization", jetonAli))
                .andExpect(status().isForbidden());
    }

    // --- Réception d'une benne ---

    @Test
    void les_bennes_d_une_tournee_pas_encore_partie_ne_peuvent_pas_etre_recues() throws Exception {
        String tournee = tourneeVers(elAmra, StatutTournee.PLANIFIEE);

        receptionner(tournee, benneDeuxTonnes, 1800, 0.6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("TOURNEE_NON_RECEVABLE")));
    }

    @Test
    void un_responsable_ne_receptionne_pas_une_tournee_qui_va_a_un_autre_pressoir() throws Exception {
        String tournee = tourneeVers(agareb, StatutTournee.LIVREE);

        receptionner(tournee, benneDeuxTonnes, 1800, 0.6)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TOURNEE_D_UN_AUTRE_PRESSOIR")));
    }

    @Test
    void une_pesee_au_dela_de_110_pourcent_de_la_benne_est_refusee() throws Exception {
        String tournee = tourneeVers(elAmra, StatutTournee.LIVREE);

        receptionner(tournee, benneDeuxTonnes, 2201, 0.6)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.poidsNetKg", containsString("plus de 110 %")));
        receptionner(tournee, benneDeuxTonnes, 2200, 0.6)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tauxRemplissage", is(110)));
    }

    @Test
    void la_reception_calcule_la_categorie_le_remplissage_et_reparti_le_poids_entre_les_vergers() throws Exception {
        String tournee = tourneeVers(elAmra, StatutTournee.EN_COURS);

        receptionner(tournee, benneDeuxTonnes, 1860, 0.6)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", is("R-0001")))
                .andExpect(jsonPath("$.categorie", is("EXTRA_VIERGE")))
                .andExpect(jsonPath("$.tauxRemplissage", is(93)))
                .andExpect(jsonPath("$.tourneeCloturee", is(false)))
                // 3 500 kg et 1 500 kg estimés : 70 % et 30 % du poids
                .andExpect(jsonPath("$.parts[0].poidsKg", is(1302)))
                .andExpect(jsonPath("$.parts[1].poidsKg", is(558)));

        receptionner(tournee, benneDeuxTonnes, 1500, 0.6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.champs.benneId", containsString("déjà été reçue")));
        mockMvc.perform(get("/api/pressoir/livraisons").header("Authorization", jetonFethi))
                .andExpect(jsonPath("$[0].recuKg", is(1860)))
                .andExpect(jsonPath("$[0].bennes[?(@.code == 'BN-01')].recue", hasItem(true)));
    }

    @Test
    void la_derniere_benne_cloture_la_tournee_et_termine_les_demandes_avec_leur_resultat() throws Exception {
        String tournee = tourneeVers(elAmra, StatutTournee.LIVREE);
        receptionner(tournee, benneDeuxTonnes, 2000, 0.4).andExpect(status().isCreated());

        receptionner(tournee, benneTroisTonnes, 3000, 1.4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorie", is("VIERGE")))
                .andExpect(jsonPath("$.tourneeCloturee", is(true)));

        assertThat(tourneeRepository.findById(tournee).orElseThrow().getStatut()).isEqualTo(StatutTournee.CLOTUREE);
        // Verger d'Ali : 70 % de chaque benne = 1 400 + 2 100 kg ; acidité pondérée (1 400 × 0,4 + 2 100 × 1,4) ÷ 3 500 = 1,0
        mockMvc.perform(get("/api/agriculteur/demandes").header("Authorization", jetonAli))
                .andExpect(jsonPath("$[0].statut", is("TERMINEE")))
                .andExpect(jsonPath("$[0].poidsRecuKg", is(3500)))
                .andExpect(jsonPath("$[0].aciditeMoyenne", is(1.0)))
                .andExpect(jsonPath("$[0].libelleCategorieQualite", is("Vierge")));
        assertThat(titresNotificationsDe(ali)).anyMatch(titre -> titre.startsWith("Olives pesées"));
        assertThat(titresNotificationsDe(karim)).anyMatch(titre -> titre.endsWith("clôturée"));
        assertThat(smsEnvoyesA(ali.getTelephone())).anyMatch(texte -> texte.contains("3500 kg de votre verger"));
    }

    // --- Capacité ---

    @Test
    void la_capacite_du_jour_compte_l_attendu_et_le_recu() throws Exception {
        String tournee = tourneeVers(elAmra, StatutTournee.LIVREE);
        receptionner(tournee, benneDeuxTonnes, 1800, 0.6).andExpect(status().isCreated());

        // 5 000 kg estimés dont 1 800 reçus : 3 200 attendus, 15 000 restants sur 20 000
        mockMvc.perform(get("/api/pressoir").header("Authorization", jetonFethi))
                .andExpect(jsonPath("$.pressoir.nom", is("Huilerie El Amra")))
                .andExpect(jsonPath("$.aujourdhui.maximumKg", is(20000)))
                .andExpect(jsonPath("$.aujourdhui.recuKg", is(1800)))
                .andExpect(jsonPath("$.aujourdhui.attenduKg", is(3200)))
                .andExpect(jsonPath("$.aujourdhui.restantKg", is(15000)));
    }

    @Test
    void la_capacite_publiee_pour_un_jour_est_utilisee_par_la_planification() throws Exception {
        LocalDate demain = AUJOURD_HUI.plusDays(1);
        mockMvc.perform(put("/api/pressoir/capacites/" + demain).header("Authorization", jetonFethi)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maximumKg\":8000,\"motif\":\"Maintenance du broyeur 2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maximumKg", is(8000)))
                .andExpect(jsonPath("$.publiee", is(true)))
                .andExpect(jsonPath("$.nomAuteur", is("Fethi Ayadi")));

        mockMvc.perform(get("/api/collecte/planification/options?date=" + demain).header("Authorization", jetonSalma))
                .andExpect(jsonPath("$.pressoirs[?(@.code == 'PR-01')].capaciteJournaliereKg", hasItem(8000)));
        mockMvc.perform(put("/api/pressoir/capacites/" + AUJOURD_HUI.minusDays(1)).header("Authorization", jetonFethi)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maximumKg\":8000}"))
                .andExpect(status().isBadRequest());
    }

    // --- Historique, export, tableau de bord ---

    @Test
    void l_historique_l_export_et_le_tableau_de_bord_reprennent_les_receptions() throws Exception {
        String tournee = tourneeVers(elAmra, StatutTournee.LIVREE);
        receptionner(tournee, benneDeuxTonnes, 2000, 0.5).andExpect(status().isCreated());
        receptionner(tournee, benneTroisTonnes, 2400, 2.5).andExpect(status().isCreated());

        mockMvc.perform(get("/api/pressoir/receptions").header("Authorization", jetonFethi))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.elements[0].codeBenne", is("BN-02")));
        mockMvc.perform(get("/api/pressoir/receptions/export?du=%s&au=%s".formatted(AUJOURD_HUI, AUJOURD_HUI))
                        .header("Authorization", jetonFethi))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Numéro;Date et heure;Tournée")))
                .andExpect(content().string(containsString(";BN-02;3000;2400;80;2,5;Courante;Fethi Ayadi")));

        // Acidité pondérée : (2 000 × 0,5 + 2 400 × 2,5) ÷ 4 400 = 1,59
        mockMvc.perform(get("/api/pressoir/tableau-de-bord?jours=7").header("Authorization", jetonFethi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux.poidsKg", is(4400)))
                .andExpect(jsonPath("$.totaux.bennes", is(2)))
                .andExpect(jsonPath("$.totaux.aciditeMoyenne", is(1.59)))
                .andExpect(jsonPath("$.parJour", hasSize(7)))
                .andExpect(jsonPath("$.parJour[6].poidsKg", is(4400)))
                .andExpect(jsonPath("$.parCategorie[?(@.categorie == 'COURANTE')].poidsKg", hasItem(2400)))
                .andExpect(jsonPath("$.parVariete[0].variete", is("Chemlali")))
                .andExpect(jsonPath("$.parVariete[0].poidsKg", is(4400)));
    }

    // --- Outils ---

    /** Tournée d'aujourd'hui vers ce pressoir avec deux vergers (3 500 et 1 500 kg) et deux bennes, dans l'état voulu. */
    private String tourneeVers(String pressoirId, StatutTournee statut) throws Exception {
        String demandeAli = demandeValidee(jetonAli, "Verger Nord", 3500);
        String demandeHela = demandeValidee(jetonHela, "Verger d'Agareb", 1500);
        String tournee = JsonPath.read(mockMvc.perform(post("/api/collecte/tournees").header("Authorization", jetonSalma)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"%s","demandeIds":["%s","%s"],"chefEquipeId":"%s","pressoirId":"%s","ressourceIds":["%s","%s"]}
                        """.formatted(AUJOURD_HUI, demandeAli, demandeHela, karim.getId(), pressoirId, benneDeuxTonnes, benneTroisTonnes)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        if (statut != StatutTournee.PLANIFIEE) {
            // Démarrage et livraison : faits plus tard par le chef d'équipe dans son espace
            Tournee enregistree = tourneeRepository.findById(tournee).orElseThrow();
            enregistree.setStatut(statut);
            tourneeRepository.save(enregistree);
        }
        return tournee;
    }

    private String demandeValidee(String jeton, String nomVerger, int quantite) throws Exception {
        String verger = JsonPath.read(mockMvc.perform(post("/api/agriculteur/vergers").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"%s","latitude":34.81,"longitude":10.77,"delegation":"SAKIET_EZZIT","superficieHa":4.5,"nombreArbres":380,"varieteId":"%s"}
                        """.formatted(nomVerger, varieteId))).andReturn().getResponse().getContentAsString(), "$.id");
        String demande = JsonPath.read(mockMvc.perform(post("/api/agriculteur/demandes").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"vergerId":"%s","type":"URGENTE","dateSouhaitee":"%s","quantiteEstimeeKg":%d}
                        """.formatted(verger, AUJOURD_HUI, quantite))).andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/collecte/demandes/" + demande + "/valider").header("Authorization", jetonSalma)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        return demande;
    }

    private ResultActions receptionner(String tournee, String benne, int poids, double acidite) throws Exception {
        return mockMvc.perform(post("/api/pressoir/receptions").header("Authorization", jetonFethi)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"tourneeId":"%s","benneId":"%s","poidsNetKg":%d,"aciditePourcent":%s}
                        """.formatted(tournee, benne, poids, acidite)));
    }

    private String pressoir(String code, String nom, int capacite) {
        Pressoir pressoir = new Pressoir();
        pressoir.setCode(code);
        pressoir.setNom(nom);
        pressoir.setDelegation(Delegation.SAKIET_EZZIT);
        pressoir.setLocalisation(new GeoJsonPoint(10.75, 34.80));
        pressoir.setCapaciteJournaliereKg(capacite);
        return pressoirRepository.save(pressoir).getId();
    }

    private String ressource(String code, int capacite) {
        Ressource benne = new Ressource();
        benne.setCode(code);
        benne.setType(TypeRessource.BENNE);
        benne.setNom("Benne " + code);
        benne.setCapaciteKg(capacite);
        benne.setEtat(EtatRessource.DISPONIBLE);
        return ressourceRepository.save(benne).getId();
    }

    private List<String> titresNotificationsDe(Utilisateur compte) {
        return mongoTemplate.find(Query.query(Criteria.where("destinataireId").is(compte.getId())), Notification.class)
                .stream().map(Notification::getTitre).toList();
    }
}
