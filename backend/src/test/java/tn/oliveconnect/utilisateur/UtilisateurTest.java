package tn.oliveconnect.utilisateur;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

/** Règles de blocage portées par le compte lui-même. */
class UtilisateurTest {

    private static final Instant MAINTENANT = Instant.parse("2026-12-02T08:00:00Z");
    private static final Duration QUINZE_MINUTES = Duration.ofMinutes(15);

    @Test
    void un_echec_augmente_le_compteur_sans_bloquer() {
        Utilisateur utilisateur = new Utilisateur();

        utilisateur.enregistrerEchecConnexion(5, QUINZE_MINUTES, MAINTENANT);

        assertThat(utilisateur.getTentativesEchouees()).isEqualTo(1);
        assertThat(utilisateur.estBloque(MAINTENANT)).isFalse();
    }

    @Test
    void le_cinquieme_echec_bloque_le_compte_quinze_minutes() {
        Utilisateur utilisateur = new Utilisateur();

        for (int i = 0; i < 5; i++) {
            utilisateur.enregistrerEchecConnexion(5, QUINZE_MINUTES, MAINTENANT);
        }

        assertThat(utilisateur.estBloque(MAINTENANT.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(utilisateur.estBloque(MAINTENANT.plus(QUINZE_MINUTES))).isFalse();
        assertThat(utilisateur.getTentativesEchouees()).isZero();
    }

    @Test
    void une_connexion_reussie_remet_les_compteurs_a_zero() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.enregistrerEchecConnexion(5, QUINZE_MINUTES, MAINTENANT);

        utilisateur.enregistrerConnexionReussie(MAINTENANT);

        assertThat(utilisateur.getTentativesEchouees()).isZero();
        assertThat(utilisateur.getBloqueJusqua()).isNull();
        assertThat(utilisateur.getDerniereConnexion()).isEqualTo(MAINTENANT);
    }
}
