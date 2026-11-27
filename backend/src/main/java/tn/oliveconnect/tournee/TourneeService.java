package tn.oliveconnect.tournee;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandeRecolteRepository;
import tn.oliveconnect.demande.StatutDemande;
import tn.oliveconnect.notification.CategorieNotification;
import tn.oliveconnect.notification.NotificationService;
import tn.oliveconnect.notification.NouvelleNotification;

/**
 * Suivi des tournées par le responsable de collecte : liste du jour, fiche, annulation.
 *
 * Choix retenus (points « à décider pendant le codage ») :
 * - une tournée ne s'annule que tant qu'elle n'a pas démarré ; ses demandes redeviennent « validées » pour être
 *   replanifiées, et les agriculteurs comme le chef d'équipe sont prévenus ;
 * - quand un agriculteur annule une demande planifiée ou en change la date, son verger sort de la tournée ;
 *   une tournée qui n'a plus aucun verger est annulée d'elle-même.
 */
@Service
@RequiredArgsConstructor
public class TourneeService {

    private final TourneeRepository tourneeRepository;
    private final DemandeRecolteRepository demandeRepository;
    private final NotificationService notificationService;
    private final Clock horloge;

    public List<TourneeReponse> duJour(LocalDate date) {
        return tourneeRepository.findByDateOrderByNumeroAsc(date).stream().map(TourneeReponse::depuis).toList();
    }

    public TourneeReponse consulter(String id) {
        return TourneeReponse.depuis(tournee(id));
    }

    public TourneeReponse annuler(String id, String motif, String auteurId) {
        Tournee tournee = tournee(id);
        if (tournee.getStatut() != StatutTournee.PLANIFIEE) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "TOURNEE_NON_ANNULABLE",
                    "Cette tournée ne peut plus être annulée : elle a démarré ou elle est close.");
        }
        String motifNettoye = motif.trim();
        tournee.setMotifAnnulation(motifNettoye);
        tournee.changerStatut(StatutTournee.ANNULEE, horloge.instant(), auteurId, motifNettoye);
        Tournee enregistree = tourneeRepository.save(tournee);

        for (Tournee.Etape etape : enregistree.getEtapes()) {
            demandeRepository.findById(etape.demandeId())
                    .filter(demande -> demande.getStatut() == StatutDemande.PLANIFIEE && id.equals(demande.getTourneeId()))
                    .ifPresent(demande -> {
                        demande.retirerDeLaTournee();
                        demande.changerStatut(StatutDemande.VALIDEE, horloge.instant(), auteurId,
                                "Tournée " + enregistree.getNumero() + " annulée : collecte à replanifier");
                        demandeRepository.save(demande);
                        notificationService.notifierPersonne(demande.getAgriculteurId(), NouvelleNotification.simple(
                                        CategorieNotification.COLLECTE, "Collecte reportée · " + demande.getNomVerger(),
                                        "La collecte prévue le " + enregistree.getDate().format(PlanificationService.FORMAT_DATE)
                                                + " est annulée (" + motifNettoye + "). Une nouvelle date vous sera proposée.",
                                        "/agriculteur/demandes")
                                .avecSms("OliveConnect : la collecte de votre verger " + demande.getNomVerger() + " prévue le "
                                        + enregistree.getDate().format(PlanificationService.FORMAT_DATE)
                                        + " est reportée. Une nouvelle date vous sera proposée."));
                    });
        }
        notificationService.notifierPersonne(enregistree.getChefEquipeId(), NouvelleNotification.simple(
                CategorieNotification.COLLECTE, "Tournée " + enregistree.getNumero() + " annulée",
                "Tournée du " + enregistree.getDate().format(PlanificationService.FORMAT_DATE) + " · Motif : " + motifNettoye,
                "/chef-equipe"));
        return TourneeReponse.depuis(enregistree);
    }

    /**
     * Sort une demande de sa tournée (annulée par l'agriculteur, ou déplacée à une autre date).
     * La demande elle-même est mise à jour et enregistrée par l'appelant.
     */
    public void retirerDemande(DemandeRecolte demande, String raison) {
        if (demande.getTourneeId() == null) {
            return;
        }
        tourneeRepository.findById(demande.getTourneeId())
                .filter(tournee -> tournee.getStatut() == StatutTournee.PLANIFIEE)
                .ifPresent(tournee -> {
                    List<Tournee.Etape> restantes = new ArrayList<>();
                    for (Tournee.Etape etape : tournee.getEtapes()) {
                        if (!etape.demandeId().equals(demande.getId())) {
                            restantes.add(etape.avecOrdre(restantes.size() + 1));
                        }
                    }
                    tournee.setEtapes(restantes);
                    String message = demande.getNomVerger() + " (" + demande.getNumero() + ") retiré : " + raison;
                    if (restantes.isEmpty()) {
                        tournee.setMotifAnnulation("Plus aucun verger à collecter");
                        tournee.changerStatut(StatutTournee.ANNULEE, horloge.instant(), demande.getAgriculteurId(),
                                "Plus aucun verger à collecter");
                        message += " · La tournée est annulée : plus aucun verger à collecter.";
                    }
                    Tournee enregistree = tourneeRepository.save(tournee);
                    notificationService.notifierPersonne(enregistree.getChefEquipeId(), NouvelleNotification.simple(
                            CategorieNotification.COLLECTE, "Tournée " + enregistree.getNumero() + " modifiée", message, "/chef-equipe"));
                });
        demande.retirerDeLaTournee();
    }

    /** Reporte dans la tournée la nouvelle quantité estimée d'une demande planifiée. */
    public void mettreAJourQuantite(DemandeRecolte demande) {
        if (demande.getTourneeId() == null) {
            return;
        }
        tourneeRepository.findById(demande.getTourneeId())
                .filter(tournee -> tournee.getStatut() == StatutTournee.PLANIFIEE)
                .ifPresent(tournee -> {
                    tournee.setEtapes(tournee.getEtapes().stream()
                            .map(etape -> etape.demandeId().equals(demande.getId()) ? etape.avecQuantite(demande.getQuantiteEstimeeKg()) : etape)
                            .toList());
                    tourneeRepository.save(tournee);
                });
    }

    private Tournee tournee(String id) {
        return tourneeRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "TOURNEE_INTROUVABLE", "Cette tournée n'existe pas."));
    }
}
