package tn.oliveconnect.demande;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Une demande de récolte telle qu'affichée à l'agriculteur, avec son historique. */
public record DemandeReponse(
        String id,
        String numero,
        String vergerId,
        String nomVerger,
        TypeRecolte type,
        String libelleType,
        LocalDate dateSouhaitee,
        int quantiteEstimeeKg,
        String commentaire,
        StatutDemande statut,
        String libelleStatut,
        boolean modifiable,
        Instant dateSoumission,
        String motifAnnulation,
        List<Etape> historique) {

    /** Un changement d'état, en toutes lettres. */
    public record Etape(String libelleAncien, String libelleNouveau, Instant date, String commentaire) {
    }

    public static DemandeReponse depuis(DemandeRecolte demande) {
        List<Etape> etapes = demande.getHistorique().stream()
                .map(changement -> new Etape(
                        changement.ancien() == null ? null : changement.ancien().getLibelle(),
                        changement.nouveau().getLibelle(), changement.date(), changement.commentaire()))
                .toList();
        return new DemandeReponse(demande.getId(), demande.getNumero(), demande.getVergerId(), demande.getNomVerger(),
                demande.getType(), demande.getType().getLibelle(), demande.getDateSouhaitee(),
                demande.getQuantiteEstimeeKg(), demande.getCommentaire(), demande.getStatut(),
                demande.getStatut().getLibelle(), demande.estModifiable(), demande.getDateSoumission(),
                demande.getMotifAnnulation(), etapes);
    }
}
