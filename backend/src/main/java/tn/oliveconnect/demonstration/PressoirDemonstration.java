package tn.oliveconnect.demonstration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.demande.DemandeRecolteRepository;
import tn.oliveconnect.demande.DemandeRecolteService;
import tn.oliveconnect.demande.DemandeRequete;
import tn.oliveconnect.demande.DemandesCollecteService;
import tn.oliveconnect.demande.StatutDemande;
import tn.oliveconnect.demande.TypeRecolte;
import tn.oliveconnect.parametres.Parametres;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.pressoir.Reception;
import tn.oliveconnect.pressoir.ReceptionRepository;
import tn.oliveconnect.referentiel.CategorieQualite;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.referentiel.PressoirRepository;
import tn.oliveconnect.ressource.TypeRessource;
import tn.oliveconnect.tournee.PlanificationRequete;
import tn.oliveconnect.tournee.PlanificationService;
import tn.oliveconnect.tournee.StatutTournee;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.tournee.TourneeRepository;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;

/**
 * Données de démonstration du responsable du pressoir Fethi Ayadi (Huilerie El Amra), en développement seulement
 * et seulement si aucune réception n'existe :
 * - un historique de réceptions sur les 6 derniers jours (tournées déjà clôturées), pour l'historique et le tableau de bord ;
 * - une tournée d'aujourd'hui vers El Amra, marquée « livrée » comme si le chef d'équipe l'avait déclarée
 *   (son espace n'existe pas encore), dont les deux bennes attendent leur pesée.
 */
@Component
@Profile("dev")
@Order(7)
@RequiredArgsConstructor
public class PressoirDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(PressoirDemonstration.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PressoirRepository pressoirRepository;
    private final VergerRepository vergerRepository;
    private final ReceptionRepository receptionRepository;
    private final TourneeRepository tourneeRepository;
    private final DemandeRecolteRepository demandeRepository;
    private final DemandeRecolteService demandeService;
    private final DemandesCollecteService demandesCollecteService;
    private final PlanificationService planificationService;
    private final ParametresService parametresService;
    private final CompteurService compteurService;
    private final Clock horloge;

    @Override
    public void run(ApplicationArguments arguments) {
        Utilisateur fethi = utilisateurRepository.findByEmail("fethi.ayadi@oliveconnect.tn").orElse(null);
        Utilisateur ali = utilisateurRepository.findByEmail("ali.bensalah@oliveconnect.tn").orElse(null);
        Utilisateur salma = utilisateurRepository.findByEmail("salma.chaabane@oliveconnect.tn").orElse(null);
        if (fethi == null || ali == null || salma == null || fethi.getPressoirId() == null || receptionRepository.count() > 0) {
            return;
        }
        Pressoir elAmra = pressoirRepository.findById(fethi.getPressoirId()).orElse(null);
        List<Verger> vergers = vergerRepository.findByAgriculteurIdOrderByNomAsc(ali.getId());
        if (elAmra == null || vergers.size() < 3) {
            return;
        }
        Utilisateur karim = utilisateurRepository.findByEmail("karim.trabelsi@oliveconnect.tn").orElse(fethi);
        historique(elAmra, vergers, fethi, karim);
        tourneeLivreeAujourdhui(elAmra, vergers, ali, salma);
        journal.info("Réceptions et tournée livrée de démonstration créées pour {}", elAmra.getNom());
    }

    /** Une tournée clôturée par jour sur les 6 derniers jours, avec deux bennes pesées. */
    private void historique(Pressoir pressoir, List<Verger> vergers, Utilisateur fethi, Utilisateur chef) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        int[][] pesees = { { 1820, 2640 }, { 1950, 1710 }, { 2880, 1460 }, { 1690, 2210 }, { 2050, 2790 }, { 1540, 1930 } };
        double[][] acidites = { { 0.5, 0.9 }, { 0.6, 0.7 }, { 1.4, 0.6 }, { 0.4, 2.4 }, { 0.7, 0.5 }, { 0.8, 1.1 } };
        for (int jour = 0; jour < pesees.length; jour++) {
            LocalDate date = aujourdhui.minusDays(6L - jour);
            Verger verger = vergers.get(jour % vergers.size());
            Tournee tournee = new Tournee();
            tournee.setNumero(compteurService.prochainCode("TOURNEE", "T-%04d"));
            tournee.setDate(date);
            tournee.setChefEquipeId(chef.getId());
            tournee.setNomChefEquipe(chef.getNomComplet());
            tournee.setPressoirId(pressoir.getId());
            tournee.setNomPressoir(pressoir.getNom());
            tournee.getEtapes().add(new Tournee.Etape(1, null, null, verger.getId(), verger.getNom(), verger.getAgriculteurId(),
                    "Ali Ben Salah", TypeRecolte.PLANIFIEE, pesees[jour][0] + pesees[jour][1]));
            tournee.getAffectations().add(new Tournee.Affectation(null, TypeRessource.BENNE, "BN-0" + (jour % 3 + 1), "Benne 2 t", 2000, null));
            tournee.getAffectations().add(new Tournee.Affectation(null, TypeRessource.BENNE, "BN-06", "Benne 3 t", 3000, null));
            tournee.setDateCreation(horloge.instant());
            tournee.setStatut(StatutTournee.CLOTUREE);
            Tournee enregistree = tourneeRepository.save(tournee);
            for (int benne = 0; benne < 2; benne++) {
                Tournee.Affectation affectation = enregistree.getAffectations().get(benne);
                Reception reception = new Reception();
                reception.setNumero(compteurService.prochainCode("RECEPTION", "R-%04d"));
                reception.setPressoirId(pressoir.getId());
                reception.setTourneeId(enregistree.getId());
                reception.setNumeroTournee(enregistree.getNumero());
                reception.setBenneId("demonstration-" + affectation.code());
                reception.setCodeBenne(affectation.code());
                reception.setCapaciteBenneKg(affectation.capaciteKg());
                reception.setPoidsNetKg(pesees[jour][benne]);
                reception.setAciditePourcent(acidites[jour][benne]);
                reception.setCategorie(CategorieQualite.pourAcidite(acidites[jour][benne]));
                reception.setTauxRemplissage((int) Math.round(pesees[jour][benne] * 100.0 / affectation.capaciteKg()));
                reception.setJour(date);
                reception.setDateHeure(date.atTime(9 + 2 * benne, 15).atZone(ZoneId.of("Africa/Tunis")).toInstant());
                reception.setAuteurId(fethi.getId());
                reception.setNomAuteur(fethi.getNomComplet());
                reception.setParts(new ArrayList<>(List.of(new Reception.Part(null, verger.getId(), verger.getNom(),
                        verger.getAgriculteurId(), pesees[jour][benne]))));
                reception.setVergerIds(List.of(verger.getId()));
                receptionRepository.save(reception);
            }
        }
    }

    /** Demande urgente d'aujourd'hui pour le Verger Est d'Ali, validée, planifiée vers El Amra, puis marquée livrée. */
    private void tourneeLivreeAujourdhui(Pressoir pressoir, List<Verger> vergers, Utilisateur ali, Utilisateur salma) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        Parametres.Campagne campagne = parametresService.actuels().campagne();
        if (aujourdhui.isBefore(campagne.debut()) || aujourdhui.isAfter(campagne.fin())) {
            return;
        }
        Verger vergerEst = vergers.stream().filter(verger -> verger.getNom().equals("Verger Est")).findFirst().orElse(vergers.getFirst());
        String demande = demandeService.declarer(new DemandeRequete(vergerEst.getId(), TypeRecolte.URGENTE, aujourdhui, 3600,
                "Olives au sol après le vent."), ali.getId()).id();
        demandesCollecteService.valider(demande, null, salma.getId());

        PlanificationService.Options options = planificationService.options(aujourdhui);
        List<String> ressources = new ArrayList<>();
        options.ressources().stream().filter(ressource -> ressource.type() == TypeRessource.TRACTEUR).findFirst()
                .ifPresent(tracteur -> ressources.add(tracteur.id()));
        options.ressources().stream().filter(ressource -> ressource.type() == TypeRessource.BENNE).limit(2)
                .forEach(benne -> ressources.add(benne.id()));
        if (options.chefs().isEmpty() || ressources.size() < 3) {
            return;
        }
        String tourneeId = planificationService.creer(new PlanificationRequete(aujourdhui, List.of(demande),
                options.chefs().getLast().id(), pressoir.getId(), ressources, "Livraison avant midi."), salma.getId()).id();

        // Démarrage et livraison : faits par le chef d'équipe dans son espace (à venir), simulés ici
        Tournee tournee = tourneeRepository.findById(tourneeId).orElseThrow();
        tournee.changerStatut(StatutTournee.EN_COURS, horloge.instant(), tournee.getChefEquipeId(), null);
        tournee.changerStatut(StatutTournee.LIVREE, horloge.instant(), tournee.getChefEquipeId(), null);
        tourneeRepository.save(tournee);
        demandeRepository.findById(demande).ifPresent(planifiee -> {
            planifiee.changerStatut(StatutDemande.EN_COURS, horloge.instant(), tournee.getChefEquipeId(), null);
            demandeRepository.save(planifiee);
        });
    }
}
