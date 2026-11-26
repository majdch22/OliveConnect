package tn.oliveconnect.referentiel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;

/**
 * Gestion des variétés d'olives par l'administrateur.
 * Une variété n'est jamais supprimée : elle est archivée, pour que les vergers et l'historique qui la citent restent lisibles.
 */
@Service
@RequiredArgsConstructor
public class VarieteService {

    /** Colonnes de tri proposées. */
    public enum Tri {
        NOM("nom"), CODE("code"), RENDEMENT("rendementMoyenKgParHectare");

        private final String champ;

        Tri(String champ) {
            this.champ = champ;
        }
    }

    private final VarieteRepository varieteRepository;
    private final CompteurService compteurService;
    private final MongoTemplate mongoTemplate;

    public List<VarieteReponse> lister(String texte, boolean avecArchives, Tri tri, Sort.Direction sens) {
        return mongoTemplate.find(RechercheReferentiel.requete(texte, "archivee", avecArchives, tri.champ, sens), Variete.class)
                .stream().map(VarieteReponse::depuis).toList();
    }

    public VarieteReponse creer(VarieteRequete requete) {
        verifierNomLibre(requete.nom(), null);
        Variete variete = new Variete();
        variete.setCode(compteurService.prochainCodeVariete());
        remplir(variete, requete);
        return VarieteReponse.depuis(varieteRepository.save(variete));
    }

    public VarieteReponse modifier(String id, VarieteRequete requete) {
        Variete variete = trouver(id);
        verifierNomLibre(requete.nom(), id);
        remplir(variete, requete);
        return VarieteReponse.depuis(varieteRepository.save(variete));
    }

    public VarieteReponse changerArchivage(String id, boolean archivee) {
        Variete variete = trouver(id);
        variete.setArchivee(archivee);
        return VarieteReponse.depuis(varieteRepository.save(variete));
    }

    private void remplir(Variete variete, VarieteRequete requete) {
        variete.setNom(requete.nom().trim());
        variete.setRendementMoyenKgParHectare(requete.rendementMoyenKgParHectare());
        variete.setMoisDebutRecolte(requete.moisDebutRecolte());
        variete.setMoisFinRecolte(requete.moisFinRecolte());
    }

    /** Deux variétés ne peuvent pas porter le même nom (sans tenir compte des majuscules). */
    private void verifierNomLibre(String nom, String varieteModifiee) {
        varieteRepository.findByNomIgnoreCase(nom.trim())
                .filter(autre -> !Objects.equals(autre.getId(), varieteModifiee))
                .ifPresent(autre -> {
                    throw new ErreurMetierException(HttpStatus.CONFLICT, "NOM_DEJA_UTILISE",
                            "Une variété porte déjà ce nom.", Map.of("nom", "Une variété porte déjà ce nom."));
                });
    }

    private Variete trouver(String id) {
        return varieteRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "VARIETE_INTROUVABLE", "Cette variété n'existe pas."));
    }
}
