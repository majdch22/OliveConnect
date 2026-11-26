package tn.oliveconnect.verger;

import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.referentiel.Variete;
import tn.oliveconnect.referentiel.VarieteRepository;

/**
 * Vergers d'un agriculteur.
 * Règle principale : un agriculteur ne voit et ne modifie que ses propres vergers (sinon refus 403).
 * Deux vergers d'un même agriculteur ne portent pas le même nom. Un verger n'est jamais supprimé : il est archivé.
 */
@Service
@RequiredArgsConstructor
public class VergerService {

    private final VergerRepository vergerRepository;
    private final VarieteRepository varieteRepository;
    private final CompteurService compteurService;

    public List<VergerReponse> mesVergers(String agriculteurId) {
        return vergerRepository.findByAgriculteurIdOrderByNomAsc(agriculteurId).stream().map(VergerReponse::depuis).toList();
    }

    public VergerReponse creer(VergerRequete requete, String agriculteurId) {
        verifierNomLibre(agriculteurId, requete.nom(), "");
        Verger verger = new Verger();
        verger.setCode(compteurService.prochainCode("VERGER", "V-%04d"));
        verger.setAgriculteurId(agriculteurId);
        remplir(verger, requete);
        return VergerReponse.depuis(vergerRepository.save(verger));
    }

    public VergerReponse modifier(String id, VergerRequete requete, String agriculteurId) {
        Verger verger = verifierProprietaire(id, agriculteurId);
        verifierNomLibre(agriculteurId, requete.nom(), id);
        remplir(verger, requete);
        return VergerReponse.depuis(vergerRepository.save(verger));
    }

    public VergerReponse changerArchivage(String id, boolean archive, String agriculteurId) {
        Verger verger = verifierProprietaire(id, agriculteurId);
        verger.setArchive(archive);
        return VergerReponse.depuis(vergerRepository.save(verger));
    }

    /**
     * Quantité d'olives attendue : superficie × rendement moyen de la variété, arrondie à la centaine de kilos.
     * Proposée à l'agriculteur quand il déclare une récolte.
     */
    public int estimationKg(String id, String agriculteurId) {
        Verger verger = verifierProprietaire(id, agriculteurId);
        int rendement = varieteRepository.findById(verger.getVarieteId())
                .map(Variete::getRendementMoyenKgParHectare).orElse(0);
        return (int) Math.round(verger.getSuperficieHa() * rendement / 100.0) * 100;
    }

    /** Le verger doit exister et appartenir à l'agriculteur connecté. */
    public Verger verifierProprietaire(String id, String agriculteurId) {
        Verger verger = vergerRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "VERGER_INTROUVABLE", "Ce verger n'existe pas."));
        if (!verger.getAgriculteurId().equals(agriculteurId)) {
            throw new ErreurMetierException(HttpStatus.FORBIDDEN, "VERGER_D_UN_AUTRE_AGRICULTEUR",
                    "Ce verger ne vous appartient pas.");
        }
        return verger;
    }

    private void remplir(Verger verger, VergerRequete requete) {
        Variete variete = varieteRepository.findById(requete.varieteId())
                .filter(trouvee -> !trouvee.isArchivee())
                .orElseThrow(() -> new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                        "Certains champs sont manquants ou incorrects.", Map.of("varieteId", "Choisissez une variété de la liste.")));
        verger.setNom(requete.nom().trim());
        // GeoJSON range les coordonnées dans l'ordre [longitude, latitude]
        verger.setLocalisation(new GeoJsonPoint(requete.longitude(), requete.latitude()));
        verger.setDelegation(requete.delegation());
        verger.setSuperficieHa(requete.superficieHa());
        verger.setNombreArbres(requete.nombreArbres());
        verger.setVarieteId(variete.getId());
        verger.setNomVariete(variete.getNom());
    }

    private void verifierNomLibre(String agriculteurId, String nom, String vergerModifie) {
        if (vergerRepository.existsByAgriculteurIdAndNomIgnoreCaseAndIdNot(agriculteurId, nom.trim(), vergerModifie)) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "NOM_DEJA_UTILISE", "Vous avez déjà un verger qui porte ce nom.",
                    Map.of("nom", "Vous avez déjà un verger qui porte ce nom."));
        }
    }
}
