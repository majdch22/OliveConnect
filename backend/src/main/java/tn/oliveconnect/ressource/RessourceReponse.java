package tn.oliveconnect.ressource;

/** Une ressource telle qu'affichée dans la liste et dans sa fiche. */
public record RessourceReponse(
        String id,
        String code,
        TypeRessource type,
        String libelleType,
        String nom,
        String matricule,
        Integer capaciteKg,
        Integer quantite,
        String equipeId,
        String nomEquipe,
        String telephone,
        EtatRessource etat,
        String libelleEtat,
        boolean archivee) {

    public static RessourceReponse depuis(Ressource ressource, String nomEquipe) {
        return new RessourceReponse(ressource.getId(), ressource.getCode(), ressource.getType(),
                ressource.getType().getLibelle(), ressource.getNom(), ressource.getMatricule(), ressource.getCapaciteKg(),
                ressource.getQuantite(), ressource.getEquipeId(), nomEquipe, ressource.getTelephone(), ressource.getEtat(),
                ressource.getEtat().getLibelle(), ressource.isArchivee());
    }
}
