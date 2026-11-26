package tn.oliveconnect.motdepasse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import tn.oliveconnect.TestAvecComptes;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/** « Mot de passe oublié » (décision 10) : code par SMS, 10 minutes, 3 essais, sans rien révéler sur les comptes. */
class MotDePasseOublieTest extends TestAvecComptes {

    private static final String TELEPHONE = "+21698123412";
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private CodeReinitialisationRepository codeRepository;

    private Utilisateur ali;

    @BeforeEach
    void preparer() {
        ali = creerAgriculteur("Ali", "Ben Salah", TELEPHONE, "01234567", Delegation.SAKIET_EZZIT, StatutCompte.ACTIF);
    }

    @Test
    void un_numero_inconnu_recoit_la_meme_reponse_mais_aucun_sms_ne_part() throws Exception {
        String reponseConnu = demanderCode("98 123 412").andReturn().getResponse().getContentAsString();
        String reponseInconnu = demanderCode("99 999 999").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(reponseInconnu).isEqualTo(reponseConnu);
        assertThat(smsEnvoyesA("+21699999999")).isEmpty();
    }

    @Test
    void le_code_recu_par_sms_permet_de_choisir_un_nouveau_mot_de_passe_et_leve_le_blocage() throws Exception {
        ali.setBloqueJusqua(Instant.now().plusSeconds(600));
        utilisateurRepository.save(ali);
        demanderCode("98123412");
        String code = dernierCode();

        verifier(code).andExpect(status().isOk());
        mockMvc.perform(post("/api/mot-de-passe-oublie/nouveau").contentType(MediaType.APPLICATION_JSON)
                        .content(nouveau(code, "NouveauMot2026")))
                .andExpect(status().isOk());

        jetonDe(TELEPHONE, "NouveauMot2026", false);
        assertThat(codeRepository.findByUtilisateurId(ali.getId())).isEmpty();
        assertThat(utilisateurRepository.findById(ali.getId()).orElseThrow().getBloqueJusqua()).isNull();
    }

    @Test
    void apres_trois_codes_faux_le_code_est_annule_meme_le_bon_ne_marche_plus() throws Exception {
        demanderCode(TELEPHONE);
        String code = dernierCode();
        String faux = code.equals("000000") ? "111111" : "000000";

        verifier(faux).andExpect(jsonPath("$.champs.code", is("Code incorrect. Il vous reste 2 essais.")));
        verifier(faux).andExpect(jsonPath("$.champs.code", is("Code incorrect. Il vous reste 1 essai.")));
        verifier(faux).andExpect(jsonPath("$.code", is("CODE_EXPIRE")));

        verifier(code).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("CODE_EXPIRE")));
    }

    @Test
    void un_code_de_plus_de_dix_minutes_est_refuse() throws Exception {
        demanderCode(TELEPHONE);
        String code = dernierCode();
        CodeReinitialisation enregistre = codeRepository.findByUtilisateurId(ali.getId()).orElseThrow();
        enregistre.setDateCreation(Instant.now().minusSeconds(11 * 60));
        codeRepository.save(enregistre);

        verifier(code).andExpect(jsonPath("$.code", is("CODE_EXPIRE")));
    }

    @Test
    void un_deuxieme_code_ne_part_pas_moins_d_une_minute_apres_le_premier() throws Exception {
        demanderCode(TELEPHONE);
        demanderCode(TELEPHONE);

        assertThat(smsEnvoyesA(TELEPHONE)).hasSize(1);
    }

    @Test
    void le_code_n_est_pas_garde_en_clair_dans_la_base() throws Exception {
        demanderCode(TELEPHONE);

        assertThat(codeRepository.findByUtilisateurId(ali.getId()).orElseThrow().getCodeChiffre()).doesNotContain(dernierCode());
    }

    private ResultActions demanderCode(String identifiant) throws Exception {
        return mockMvc.perform(post("/api/mot-de-passe-oublie/code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifiant\":\"%s\"}".formatted(identifiant)));
    }

    private ResultActions verifier(String code) throws Exception {
        return mockMvc.perform(post("/api/mot-de-passe-oublie/verification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifiant\":\"%s\",\"code\":\"%s\"}".formatted(TELEPHONE, code)));
    }

    private static String nouveau(String code, String motDePasse) {
        return "{\"identifiant\":\"%s\",\"code\":\"%s\",\"nouveauMotDePasse\":\"%s\",\"confirmation\":\"%s\"}"
                .formatted(TELEPHONE, code, motDePasse, motDePasse);
    }

    /** Le code lu dans le dernier SMS simulé, comme le ferait la personne sur son téléphone. */
    private String dernierCode() {
        List<String> messages = smsEnvoyesA(TELEPHONE);
        Matcher trouve = CODE.matcher(messages.get(messages.size() - 1));
        assertThat(trouve.find()).isTrue();
        return trouve.group(1);
    }
}
