package tn.oliveconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Sécurité des jetons d'accès et des réponses : un jeton falsifié, expiré ou d'un autre émetteur est refusé,
 * les refus sont au format JSON, les en-têtes de protection sont présents, le mot de passe se change en sécurité.
 */
class SecuriteTest extends TestAvecComptes {

    /** Secret du profil de test (application-test.yml). */
    private static final String SECRET_DES_TESTS = "secret-des-tests-oliveconnect-au-moins-32-caracteres";

    private Utilisateur chef;

    @BeforeEach
    void preparer() {
        chef = creerPersonnel("Karim", "Trabelsi", "+21698200002", Role.CHEF_EQUIPE, StatutCompte.ACTIF);
    }

    @Test
    void sans_jeton_la_reponse_est_un_401_au_format_json() throws Exception {
        mockMvc.perform(get("/api/profil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("NON_CONNECTE")));
    }

    @Test
    void un_jeton_signe_avec_un_autre_secret_est_refuse() throws Exception {
        String jeton = jeton("un-autre-secret-que-celui-du-serveur-oliveconnect", "oliveconnect", Instant.now().plusSeconds(600));

        mockMvc.perform(get("/api/profil").header("Authorization", "Bearer " + jeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void un_jeton_d_un_autre_emetteur_est_refuse() throws Exception {
        String jeton = jeton(SECRET_DES_TESTS, "un-autre-site", Instant.now().plusSeconds(600));

        mockMvc.perform(get("/api/profil").header("Authorization", "Bearer " + jeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void un_jeton_expire_est_refuse() throws Exception {
        String jeton = jeton(SECRET_DES_TESTS, "oliveconnect", Instant.now().minusSeconds(600));

        mockMvc.perform(get("/api/profil").header("Authorization", "Bearer " + jeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void un_jeton_valide_fabrique_avec_le_bon_secret_est_accepte() throws Exception {
        String jeton = jeton(SECRET_DES_TESTS, "oliveconnect", Instant.now().plusSeconds(600));

        mockMvc.perform(get("/api/profil").header("Authorization", "Bearer " + jeton)).andExpect(status().isOk());
    }

    @Test
    void les_en_tetes_de_protection_sont_presents() throws Exception {
        mockMvc.perform(get("/api/profil").header("Authorization", jetonDe(chef)))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
    }

    @Test
    void une_valeur_de_parametre_inconnue_donne_un_message_clair() throws Exception {
        String jetonAdministrateur = jetonDe(creerPersonnel("Admin", "Principal", "+21670000010", Role.ADMINISTRATEUR, StatutCompte.ACTIF));

        mockMvc.perform(get("/api/administration/utilisateurs").param("tri", "INCONNU").header("Authorization", jetonAdministrateur))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAMETRE_INVALIDE")));
    }

    @Test
    void changer_son_mot_de_passe_exige_l_ancien_et_retire_le_caractere_provisoire() throws Exception {
        chef.setMotDePasseProvisoire(true);
        utilisateurRepository.save(chef);
        String jetonChef = jetonDe(chef);

        mockMvc.perform(put("/api/profil/mot-de-passe").header("Authorization", jetonChef).contentType(MediaType.APPLICATION_JSON)
                        .content(changement("mauvais", "Nouveau2026", "Nouveau2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motDePasseActuel").exists());

        mockMvc.perform(put("/api/profil/mot-de-passe").header("Authorization", jetonChef).contentType(MediaType.APPLICATION_JSON)
                        .content(changement(MOT_DE_PASSE, "Nouveau2026", "Nouveau2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motDePasseProvisoire", is(false)));

        String reponse = mockMvc.perform(post("/api/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"+21698200002\",\"motDePasse\":\"Nouveau2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((Boolean) JsonPath.read(reponse, "$.motDePasseProvisoire")).isFalse();
    }

    // --- Outils des tests ---

    /** Fabrique un jeton signé « à la main », pour vérifier ce que le serveur accepte ou refuse. */
    private String jeton(String secret, String emetteur, Instant expiration) {
        var encodeur = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet informations = JwtClaimsSet.builder()
                .issuer(emetteur)
                .subject(chef.getId())
                .issuedAt(expiration.minusSeconds(3600))
                .expiresAt(expiration)
                .claim(JetonService.INFORMATION_ROLE, "CHEF_EQUIPE")
                .build();
        return encodeur.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), informations)).getTokenValue();
    }

    private static String changement(String actuel, String nouveau, String confirmation) {
        return """
                {"motDePasseActuel":"%s","nouveauMotDePasse":"%s","confirmation":"%s"}
                """.formatted(actuel, nouveau, confirmation);
    }
}
