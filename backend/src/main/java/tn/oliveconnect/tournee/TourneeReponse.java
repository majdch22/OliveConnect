package tn.oliveconnect.tournee;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Une tournée telle qu'affichée au responsable de collecte, avec ses totaux et son historique. */
public record TourneeReponse(
        String id,
        String numero,
        LocalDate date,
        StatutTournee statut,
        String libelleStatut,
        String chefEquipeId,
        String nomChefEquipe,
        String pressoirId,
        String nomPressoir,
        List<Tournee.Etape> etapes,
        List<Tournee.Affectation> affectations,
        int quantiteEstimeeKg,
        int capaciteBennesKg,
        String commentaire,
        String motifAnnulation,
        boolean annulable,
        Instant dateCreation,
        List<Etape> historique) {

    /** Un changement d'état, en toutes lettres. */
    public record Etape(String libelleNouveau, Instant date, String commentaire) {
    }

    public static TourneeReponse depuis(Tournee tournee) {
        return new TourneeReponse(tournee.getId(), tournee.getNumero(), tournee.getDate(), tournee.getStatut(),
                tournee.getStatut().getLibelle(), tournee.getChefEquipeId(), tournee.getNomChefEquipe(),
                tournee.getPressoirId(), tournee.getNomPressoir(), tournee.getEtapes(), tournee.getAffectations(),
                tournee.quantiteEstimeeKg(), tournee.capaciteBennesKg(), tournee.getCommentaire(),
                tournee.getMotifAnnulation(), tournee.getStatut() == StatutTournee.PLANIFIEE, tournee.getDateCreation(),
                tournee.getHistorique().stream()
                        .map(changement -> new Etape(changement.nouveau().getLibelle(), changement.date(), changement.commentaire()))
                        .toList());
    }
}
