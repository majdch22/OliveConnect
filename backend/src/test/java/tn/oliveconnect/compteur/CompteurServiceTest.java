package tn.oliveconnect.compteur;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Numéros lisibles distribués par MongoDB, sans doublon. */
@SpringBootTest
@ActiveProfiles("test")
class CompteurServiceTest {

    @Autowired
    private CompteurService compteurService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void viderLesCompteurs() {
        mongoTemplate.dropCollection(Compteur.class);
    }

    @Test
    void le_premier_numero_d_adherent_est_ADH_0001_puis_les_suivants_se_suivent() {
        assertThat(compteurService.prochainNumeroAdherent()).isEqualTo("ADH-0001");
        assertThat(compteurService.prochainNumeroAdherent()).isEqualTo("ADH-0002");
    }

    @Test
    void le_compteur_repart_apres_les_numeros_deja_attribues() {
        compteurService.garantirMinimum(CompteurService.ADHERENT, 42);

        assertThat(compteurService.prochainNumeroAdherent()).isEqualTo("ADH-0043");
    }

    @Test
    void garantir_un_minimum_ne_fait_jamais_reculer_le_compteur() {
        compteurService.garantirMinimum(CompteurService.ADHERENT, 50);
        compteurService.garantirMinimum(CompteurService.ADHERENT, 10);

        assertThat(compteurService.prochainNumeroAdherent()).isEqualTo("ADH-0051");
    }
}
