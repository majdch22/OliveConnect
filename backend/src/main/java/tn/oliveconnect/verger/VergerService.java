package tn.oliveconnect.verger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.alerte.AlerteRepository;
import tn.oliveconnect.alerte.StatutAlerte;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandeRecolteRepository;
import tn.oliveconnect.referentiel.Variete;
import tn.oliveconnect.referentiel.VarieteRepository;

/**
 * Vergers d'un agriculteur.
 * Règle principale : un agriculteur ne voit et ne modifie que ses propres vergers (sinon refus 403).
 * Deux vergers d'un même agriculteur ne portent pas le même nom.
 * Un verger qui a un historique (demandes, signalements) n'est jamais supprimé, il est archivé ;
 * un verger jamais utilisé (ajouté par erreur) peut être supprimé.
 * Le nom du verger est recopié dans ses demandes et signalements pour l'affichage : un changement de nom y est reporté.
 */
@Service
@RequiredArgsConstructor
public class VergerService {

    private final VergerRepository vergerRepository;
    private final VarieteRepository varieteRepository;
    private final DemandeRecolteRepository demandeRepository;
    private final AlerteRepository alerteRepository;
    private final CompteurService compteurService;
    private final MongoTemplate mongoTemplate;

    /** Mes vergers, chacun avec le nombre de ses signalements pas encore résolus. */
    public List<VergerReponse> mesVergers(String agriculteurId) {
        Map<String, Long> nonResolusParVerger = alerteRepository
                .findByAgriculteurIdAndStatutIn(agriculteurId, StatutAlerte.NON_RESOLUS).stream()
                .collect(Collectors.groupingBy(Alerte::getVergerId, Collectors.counting()));
        return vergerRepository.findByAgriculteurIdOrderByNomAsc(agriculteurId).stream()
                .map(verger -> VergerReponse.depuis(verger, nonResolusParVerger.getOrDefault(verger.getId(), 0L).intValue()))
                .toList();
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
        String ancienNom = verger.getNom();
        remplir(verger, requete);
        Verger enregistre = vergerRepository.save(verger);
        if (!enregistre.getNom().equals(ancienNom)) {
            reporterNouveauNom(enregistre);
        }
        return VergerReponse.depuis(enregistre);
    }

    /** Suppression réelle, seulement pour un verger sans aucune demande ni aucun signalement (sinon 409 : l'archiver). */
    public void supprimer(String id, String agriculteurId) {
        Verger verger = verifierProprietaire(id, agriculteurId);
        if (demandeRepository.existsByVergerId(id) || alerteRepository.existsByVergerId(id)) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "VERGER_DEJA_UTILISE",
                    "Ce verger a déjà des demandes ou des signalements : archivez-le pour garder son historique.");
        }
        vergerRepository.delete(verger);
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

    /** Recopie le nouveau nom dans les demandes et signalements du verger (une requête par collection). */
    private void reporterNouveauNom(Verger verger) {
        Query duVerger = Query.query(Criteria.where("vergerId").is(verger.getId()));
        Update nouveauNom = Update.update("nomVerger", verger.getNom());
        mongoTemplate.updateMulti(duVerger, nouveauNom, DemandeRecolte.class);
        mongoTemplate.updateMulti(duVerger, nouveauNom, Alerte.class);
    }

    private void verifierNomLibre(String agriculteurId, String nom, String vergerModifie) {
        if (vergerRepository.existsByAgriculteurIdAndNomIgnoreCaseAndIdNot(agriculteurId, nom.trim(), vergerModifie)) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "NOM_DEJA_UTILISE", "Vous avez déjà un verger qui porte ce nom.",
                    Map.of("nom", "Vous avez déjà un verger qui porte ce nom."));
        }
    }
}
