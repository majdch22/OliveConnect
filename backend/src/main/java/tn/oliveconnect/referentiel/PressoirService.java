package tn.oliveconnect.referentiel;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.utilisateur.Identifiants;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Gestion des pressoirs par l'administrateur.
 * Un pressoir n'est jamais supprimé : il est archivé, pour que les réceptions passées restent lisibles.
 */
@Service
@RequiredArgsConstructor
public class PressoirService {

    /** Colonnes de tri proposées. */
    public enum Tri {
        NOM("nom"), CODE("code"), CAPACITE("capaciteJournaliereKg"), DELEGATION("delegation");

        private final String champ;

        Tri(String champ) {
            this.champ = champ;
        }
    }

    private final PressoirRepository pressoirRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CompteurService compteurService;
    private final MongoTemplate mongoTemplate;

    public List<PressoirReponse> lister(String texte, boolean avecArchives, Tri tri, Sort.Direction sens) {
        return mongoTemplate.find(RechercheReferentiel.requete(texte, "archive", avecArchives, tri.champ, sens), Pressoir.class)
                .stream().map(this::reponse).toList();
    }

    public PressoirReponse creer(PressoirRequete requete) {
        Pressoir pressoir = new Pressoir();
        pressoir.setCode(compteurService.prochainCodePressoir());
        remplir(pressoir, requete);
        return reponse(pressoirRepository.save(pressoir));
    }

    public PressoirReponse modifier(String id, PressoirRequete requete) {
        Pressoir pressoir = trouver(id);
        remplir(pressoir, requete);
        return reponse(pressoirRepository.save(pressoir));
    }

    /** Un pressoir qui a encore des responsables rattachés ne peut pas être archivé. */
    public PressoirReponse changerArchivage(String id, boolean archive) {
        Pressoir pressoir = trouver(id);
        if (archive && utilisateurRepository.countByPressoirId(id) > 0) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "PRESSOIR_ENCORE_UTILISE",
                    "Des responsables sont encore rattachés à ce pressoir : rattachez-les à un autre pressoir d'abord.");
        }
        pressoir.setArchive(archive);
        return reponse(pressoirRepository.save(pressoir));
    }

    private void remplir(Pressoir pressoir, PressoirRequete requete) {
        pressoir.setNom(requete.nom().trim());
        pressoir.setDelegation(requete.delegation());
        // GeoJSON range les coordonnées dans l'ordre [longitude, latitude]
        pressoir.setLocalisation(new GeoJsonPoint(requete.longitude(), requete.latitude()));
        pressoir.setCapaciteJournaliereKg(requete.capaciteJournaliereKg());
        pressoir.setTelephone(requete.telephone() == null || requete.telephone().isBlank()
                ? null
                : Identifiants.normaliserTelephone(requete.telephone()));
    }

    private PressoirReponse reponse(Pressoir pressoir) {
        return PressoirReponse.depuis(pressoir, utilisateurRepository.countByPressoirId(pressoir.getId()));
    }

    private Pressoir trouver(String id) {
        return pressoirRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "PRESSOIR_INTROUVABLE", "Ce pressoir n'existe pas."));
    }
}
