package tn.oliveconnect.demonstration;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.alerte.AlerteService;
import tn.oliveconnect.alerte.SignalementRequete;
import tn.oliveconnect.alerte.TypeAlerte;
import tn.oliveconnect.demande.DemandeRecolteService;
import tn.oliveconnect.demande.DemandeRequete;
import tn.oliveconnect.demande.TypeRecolte;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.referentiel.VarieteRepository;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.VergerRepository;
import tn.oliveconnect.verger.VergerRequete;
import tn.oliveconnect.verger.VergerService;

/**
 * Données de démonstration de l'agriculteur Ali Ben Salah (profil « dev », seulement s'il n'a encore aucun verger) :
 * trois vergers, deux demandes de récolte et un signalement, pour que son espace ne soit pas vide.
 */
@Component
@Profile("dev")
@Order(5)
@RequiredArgsConstructor
public class AgriculteurDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(AgriculteurDemonstration.class);

    private final UtilisateurRepository utilisateurRepository;
    private final VergerRepository vergerRepository;
    private final VergerService vergerService;
    private final VarieteRepository varieteRepository;
    private final DemandeRecolteService demandeService;
    private final AlerteService alerteService;
    private final ParametresService parametresService;
    private final Clock horloge;

    @Override
    public void run(ApplicationArguments arguments) {
        String ali = utilisateurRepository.findByEmail("ali.bensalah@oliveconnect.tn").map(compte -> compte.getId()).orElse(null);
        if (ali == null || vergerRepository.countByAgriculteurIdAndArchiveFalse(ali) > 0) {
            return;
        }
        String chemlali = varieteRepository.findByNomIgnoreCase("Chemlali").map(variete -> variete.getId()).orElse(null);
        String chetoui = varieteRepository.findByNomIgnoreCase("Chetoui").map(variete -> variete.getId()).orElse(chemlali);
        if (chemlali == null) {
            return;
        }
        String nord = vergerService.creer(new VergerRequete("Verger Nord", 34.8150, 10.7650, Delegation.SAKIET_EZZIT, 4.5, 380, chemlali), ali).id();
        vergerService.creer(new VergerRequete("Verger Sud", 34.7990, 10.7720, Delegation.SAKIET_EZZIT, 3.0, 240, chetoui), ali);
        vergerService.creer(new VergerRequete("Verger Est", 34.8080, 10.7900, Delegation.SAKIET_EZZIT, 2.2, 190, chemlali), ali);

        // Les demandes doivent tomber dans la campagne de récolte des paramètres
        Parametres.Campagne campagne = parametresService.actuels().campagne();
        LocalDate demain = LocalDate.now(horloge).plusDays(1);
        if (!demain.isBefore(campagne.debut()) && demain.plusDays(5).isBefore(campagne.fin())) {
            demandeService.declarer(new DemandeRequete(nord, TypeRecolte.URGENTE, demain, 3500,
                    "Accès par la piste nord."), ali);
            demandeService.declarer(new DemandeRequete(nord, TypeRecolte.PLANIFIEE, demain.plusDays(5), 2000, null), ali);
        }
        alerteService.signaler(new SignalementRequete(nord, TypeAlerte.RAVAGEUR, 34.8152, 10.7653,
                "Mouche de l'olive sur la rangée est, environ 30 arbres."), ali);
        journal.info("Vergers, demandes et signalement de démonstration créés pour Ali Ben Salah");
    }
}
