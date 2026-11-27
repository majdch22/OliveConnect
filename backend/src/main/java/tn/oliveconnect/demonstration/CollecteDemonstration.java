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
import tn.oliveconnect.demande.DemandesCollecteService;
import tn.oliveconnect.demande.TypeRecolte;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.referentiel.VarieteRepository;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;
import tn.oliveconnect.verger.VergerRequete;
import tn.oliveconnect.verger.VergerService;

/**
 * Données de démonstration du responsable de collecte (profil « dev », seulement si Mounir Karray n'a encore aucun verger) :
 * vergers de plusieurs agriculteurs avec une maturité mesurée, demandes soumises (dont une urgente) et validées,
 * et deux signalements, pour que la file des demandes, la planification, les alertes et la carte aient de quoi s'afficher.
 */
@Component
@Profile("dev")
@Order(6)
@RequiredArgsConstructor
public class CollecteDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(CollecteDemonstration.class);

    private final UtilisateurRepository utilisateurRepository;
    private final VergerRepository vergerRepository;
    private final VergerService vergerService;
    private final VarieteRepository varieteRepository;
    private final DemandeRecolteService demandeService;
    private final DemandesCollecteService demandesCollecteService;
    private final AlerteService alerteService;
    private final ParametresService parametresService;
    private final Clock horloge;

    @Override
    public void run(ApplicationArguments arguments) {
        Utilisateur mounir = compte("mounir.karray@oliveconnect.tn");
        Utilisateur hela = compte("hela.frikha@oliveconnect.tn");
        Utilisateur youssef = compte("youssef.ellouze@oliveconnect.tn");
        Utilisateur salma = compte("salma.chaabane@oliveconnect.tn");
        if (mounir == null || hela == null || youssef == null || salma == null
                || vergerRepository.countByAgriculteurIdAndArchiveFalse(mounir.getId()) > 0) {
            return;
        }
        String chemlali = varieteRepository.findByNomIgnoreCase("Chemlali").map(variete -> variete.getId()).orElse(null);
        if (chemlali == null) {
            return;
        }
        String henchaDeux = verger(mounir, "El Hencha 2", 34.9420, 10.4650, Delegation.EL_HENCHA, 5.2, 410, chemlali, 68);
        String henchaUn = verger(mounir, "El Hencha 1", 34.9310, 10.4500, Delegation.EL_HENCHA, 3.1, 260, chemlali, 74);
        String agareb = verger(hela, "Agareb 1", 34.7420, 10.5280, Delegation.AGAREB, 2.8, 230, chemlali, 82);
        String thyna = verger(youssef, "Oliveraie de Thyna", 34.6720, 10.7050, Delegation.THYNA, 6.0, 520, chemlali, 61);

        Parametres.Campagne campagne = parametresService.actuels().campagne();
        LocalDate aujourdhui = LocalDate.now(horloge);
        if (!aujourdhui.isBefore(campagne.debut()) && aujourdhui.plusDays(8).isBefore(campagne.fin())) {
            demandeService.declarer(new DemandeRequete(agareb, TypeRecolte.URGENTE, aujourdhui.plusDays(1), 2300,
                    "Olives très mûres, chute importante."), hela.getId());
            demandeService.declarer(new DemandeRequete(henchaDeux, TypeRecolte.PLANIFIEE, aujourdhui.plusDays(3), 4100,
                    "Prévoir accès par la piste nord."), mounir.getId());
            demandeService.declarer(new DemandeRequete(thyna, TypeRecolte.PLANIFIEE, aujourdhui.plusDays(6), 5000, null),
                    youssef.getId());
            // Deux demandes déjà validées par Salma Chaabane : elles attendent une tournée
            String henchaValidee = demandeService.declarer(new DemandeRequete(henchaUn, TypeRecolte.PLANIFIEE,
                    aujourdhui.plusDays(2), 1200, null), mounir.getId()).id();
            String agarebValidee = demandeService.declarer(new DemandeRequete(agareb, TypeRecolte.PLANIFIEE,
                    aujourdhui.plusDays(2), 1500, null), hela.getId()).id();
            demandesCollecteService.valider(henchaValidee, null, salma.getId());
            demandesCollecteService.valider(agarebValidee, null, salma.getId());
        }
        alerteService.signaler(new SignalementRequete(agareb, TypeAlerte.MATURITE, 34.7422, 10.5283,
                "Maturité avancée, les olives tombent déjà."), hela.getId());
        alerteService.signaler(new SignalementRequete(thyna, TypeAlerte.PLUIE, 34.6735, 10.7040,
                "Fortes pluies annoncées, accès boueux par l'ouest."), youssef.getId());
        journal.info("Vergers, demandes et signalements de démonstration créés pour l'espace du responsable de collecte");
    }

    private Utilisateur compte(String email) {
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    /** Crée le verger puis lui donne une maturité, comme si les capteurs l'avaient mesurée. */
    private String verger(Utilisateur agriculteur, String nom, double latitude, double longitude, Delegation delegation,
            double superficie, int arbres, String varieteId, int maturite) {
        String id = vergerService.creer(new VergerRequete(nom, latitude, longitude, delegation, superficie, arbres, varieteId),
                agriculteur.getId()).id();
        Verger verger = vergerRepository.findById(id).orElseThrow();
        verger.setNiveauMaturite(maturite);
        vergerRepository.save(verger);
        return id;
    }
}
