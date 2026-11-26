package tn.oliveconnect.referentiel;

import tn.oliveconnect.utilisateur.Delegation;

/**
 * Un pressoir tel qu'affiché dans la liste.
 *
 * @param nombreResponsables nombre de comptes « responsable du pressoir » rattachés à ce pressoir
 */
public record PressoirReponse(
        String id,
        String code,
        String nom,
        Delegation delegation,
        String libelleDelegation,
        double latitude,
        double longitude,
        int capaciteJournaliereKg,
        String telephone,
        boolean archive,
        long nombreResponsables) {

    public static PressoirReponse depuis(Pressoir pressoir, long nombreResponsables) {
        return new PressoirReponse(pressoir.getId(), pressoir.getCode(), pressoir.getNom(), pressoir.getDelegation(),
                pressoir.getDelegation().getLibelle(),
                // GeoJSON range les coordonnées dans l'ordre [longitude, latitude]
                pressoir.getLocalisation().getY(), pressoir.getLocalisation().getX(),
                pressoir.getCapaciteJournaliereKg(), pressoir.getTelephone(), pressoir.isArchive(), nombreResponsables);
    }
}
