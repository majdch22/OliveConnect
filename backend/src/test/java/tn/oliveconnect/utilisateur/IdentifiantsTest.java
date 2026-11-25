package tn.oliveconnect.utilisateur;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Les différentes façons d'écrire un même téléphone ou e-mail mènent au même compte. */
class IdentifiantsTest {

    @Test
    void un_numero_tunisien_a_huit_chiffres_recoit_l_indicatif() {
        assertThat(Identifiants.normaliserTelephone("98 123 412")).isEqualTo("+21698123412");
    }

    @Test
    void les_ecritures_avec_indicatif_donnent_le_meme_numero() {
        assertThat(Identifiants.normaliserTelephone("+216 98-123-412")).isEqualTo("+21698123412");
        assertThat(Identifiants.normaliserTelephone("00216 98 123 412")).isEqualTo("+21698123412");
    }

    @Test
    void l_email_est_mis_en_minuscules_sans_espaces() {
        assertThat(Identifiants.normaliserEmail("  Ali.BenSalah@Exemple.TN ")).isEqualTo("ali.bensalah@exemple.tn");
    }

    @Test
    void un_identifiant_avec_arobase_est_un_email() {
        assertThat(Identifiants.estEmail("ali@exemple.tn")).isTrue();
        assertThat(Identifiants.estEmail("98123412")).isFalse();
    }
}
