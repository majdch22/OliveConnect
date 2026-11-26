package tn.oliveconnect.verger;

import tn.oliveconnect.utilisateur.Delegation;

/** Un verger tel qu'affiché à son agriculteur. */
public record VergerReponse(
        String id,
        String code,
        String nom,
        double latitude,
        double longitude,
        Delegation delegation,
        String libelleDelegation,
        double superficieHa,
        int nombreArbres,
        String varieteId,
        String nomVariete,
        Integer niveauMaturite,
        StatutRecolte statutRecolte,
        String libelleStatutRecolte,
        boolean archive) {

    public static VergerReponse depuis(Verger verger) {
        return new VergerReponse(verger.getId(), verger.getCode(), verger.getNom(),
                // GeoJSON range les coordonnées dans l'ordre [longitude, latitude]
                verger.getLocalisation().getY(), verger.getLocalisation().getX(),
                verger.getDelegation(), verger.getDelegation().getLibelle(), verger.getSuperficieHa(),
                verger.getNombreArbres(), verger.getVarieteId(), verger.getNomVariete(), verger.getNiveauMaturite(),
                verger.getStatutRecolte(), verger.getStatutRecolte().getLibelle(), verger.isArchive());
    }
}
