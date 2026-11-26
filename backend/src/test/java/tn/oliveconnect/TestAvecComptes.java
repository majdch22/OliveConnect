package tn.oliveconnect;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import tn.oliveconnect.compteur.Compteur;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.referentiel.Variete;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.InfosAgriculteur;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Base des tests de bout en bout : vraie requête, vraie sécurité, vraie base MongoDB de test.
 * La base est vidée avant chaque test ; chaque test crée les comptes dont il a besoin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TestAvecComptes {

    protected static final String MOT_DE_PASSE = "Olive2026!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder encodeurMotDePasse;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    private ParametresService parametresService;

    /** Chaque test part d'une base vide et des paramètres par défaut. */
    @BeforeEach
    void viderLaBase() {
        utilisateurRepository.deleteAll();
        mongoTemplate.dropCollection(Compteur.class);
        mongoTemplate.dropCollection(Parametres.class);
        // Vidées (et non supprimées) pour garder leurs index : codes uniques, index géographique
        mongoTemplate.remove(new Query(), Variete.class);
        mongoTemplate.remove(new Query(), Pressoir.class);
        parametresService.relireDepuisLaBase();
    }

    protected Utilisateur creerPersonnel(String prenom, String nom, String telephone, Role role, StatutCompte statut) {
        Utilisateur compte = new Utilisateur();
        compte.setPrenom(prenom);
        compte.setNom(nom);
        compte.setEmail(prenom.toLowerCase() + "@test.tn");
        compte.setTelephone(telephone);
        compte.setMotDePasse(encodeurMotDePasse.encode(MOT_DE_PASSE));
        compte.setRole(role);
        compte.setStatut(statut);
        return utilisateurRepository.save(compte);
    }

    protected Utilisateur creerAgriculteur(String prenom, String nom, String telephone, String cin,
            Delegation delegation, StatutCompte statut) {
        Utilisateur compte = creerPersonnel(prenom, nom, telephone, Role.AGRICULTEUR, statut);
        InfosAgriculteur infos = new InfosAgriculteur();
        infos.setCin(cin);
        infos.setDelegation(delegation);
        compte.setAgriculteur(infos);
        return utilisateurRepository.save(compte);
    }

    /** Se connecte sur la bonne page selon le rôle et renvoie l'en-tête « Bearer ... » à joindre aux requêtes. */
    protected String jetonDe(Utilisateur compte) throws Exception {
        return jetonDe(compte.getTelephone(), MOT_DE_PASSE, compte.getRole() == Role.ADMINISTRATEUR);
    }

    protected String jetonDe(String identifiant, String motDePasse, boolean pageAdministrateur) throws Exception {
        String adresse = pageAdministrateur ? "/api/administration/connexion" : "/api/connexion";
        String reponse = mockMvc.perform(post(adresse).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"%s\",\"motDePasse\":\"%s\"}".formatted(identifiant, motDePasse)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(reponse, "$.jeton");
    }
}
