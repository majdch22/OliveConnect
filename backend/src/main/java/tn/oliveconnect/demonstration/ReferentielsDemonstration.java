package tn.oliveconnect.demonstration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.referentiel.PressoirRepository;
import tn.oliveconnect.referentiel.PressoirRequete;
import tn.oliveconnect.referentiel.PressoirService;
import tn.oliveconnect.referentiel.VarieteRepository;
import tn.oliveconnect.referentiel.VarieteRequete;
import tn.oliveconnect.referentiel.VarieteService;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Référentiels de démonstration, créés seulement en développement (profil « dev ») et seulement si les collections sont vides.
 * En production, ces données arrivent par l'import des référentiels de la coopérative.
 */
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
public class ReferentielsDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(ReferentielsDemonstration.class);

    /** Principales variétés tunisiennes : rendement moyen (kg/ha) et mois de récolte. */
    private static final List<VarieteRequete> VARIETES = List.of(
            new VarieteRequete("Chemlali", 3000, 11, 2),
            new VarieteRequete("Chetoui", 3500, 10, 12),
            new VarieteRequete("Zalmati", 2800, 11, 1),
            new VarieteRequete("Oueslati", 3200, 10, 1),
            new VarieteRequete("Zarrazi", 2600, 11, 2),
            new VarieteRequete("Chemchali", 2900, 11, 2));

    /** Pressoirs autour de Sfax (positions approximatives). */
    private static final List<PressoirRequete> PRESSOIRS = List.of(
            new PressoirRequete("Huilerie El Amra", Delegation.EL_AMRA, 34.990, 10.920, 20000, "74200100"),
            new PressoirRequete("Huilerie d'Agareb", Delegation.AGAREB, 34.740, 10.530, 15000, "74200200"),
            new PressoirRequete("Huilerie de Sakiet Ezzit", Delegation.SAKIET_EZZIT, 34.810, 10.770, 25000, "74200300"),
            new PressoirRequete("Huilerie d'El Hencha", Delegation.EL_HENCHA, 35.120, 10.740, 12000, "74200400"));

    private final VarieteRepository varieteRepository;
    private final VarieteService varieteService;
    private final PressoirRepository pressoirRepository;
    private final PressoirService pressoirService;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    public void run(ApplicationArguments arguments) {
        if (varieteRepository.count() == 0) {
            VARIETES.forEach(varieteService::creer);
            journal.info("{} variétés de démonstration créées", VARIETES.size());
        }
        if (pressoirRepository.count() == 0) {
            PRESSOIRS.forEach(pressoirService::creer);
            journal.info("{} pressoirs de démonstration créés", PRESSOIRS.size());
        }
        rattacherResponsableDePressoir();
    }

    /** Le responsable du pressoir de démonstration (Fethi Ayadi) est rattaché au premier pressoir. */
    private void rattacherResponsableDePressoir() {
        String premierPressoir = pressoirRepository.findAll().stream()
                .filter(pressoir -> "PR-01".equals(pressoir.getCode()))
                .map(pressoir -> pressoir.getId())
                .findFirst().orElse(null);
        utilisateurRepository.findByEmail("fethi.ayadi@oliveconnect.tn")
                .filter(responsable -> responsable.getPressoirId() == null && premierPressoir != null)
                .ifPresent(responsable -> {
                    responsable.setPressoirId(premierPressoir);
                    utilisateurRepository.save(responsable);
                });
    }
}
