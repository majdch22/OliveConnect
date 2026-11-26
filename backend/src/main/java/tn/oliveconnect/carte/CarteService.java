package tn.oliveconnect.carte;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.referentiel.PressoirReponse;
import tn.oliveconnect.referentiel.PressoirService;
import tn.oliveconnect.utilisateur.Delegation;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Données de la carte de l'administration : les pressoirs et le nombre d'agriculteurs par délégation.
 * Le comptage est fait par MongoDB en une seule agrégation (regroupement par délégation et par état),
 * au lieu d'une requête par délégation. Les vergers s'ajouteront à la carte avec leur fonctionnalité.
 */
@Service
@RequiredArgsConstructor
public class CarteService {

    /** Une délégation placée sur la carte, avec ses agriculteurs actifs et ses inscriptions en attente. */
    public record DelegationSurCarte(Delegation code, String libelle, double latitude, double longitude,
            long agriculteursActifs, long inscriptionsEnAttente) {
    }

    public record DonneesCarte(List<PressoirReponse> pressoirs, List<DelegationSurCarte> delegations) {
    }

    private final MongoTemplate mongoTemplate;
    private final PressoirService pressoirService;

    public DonneesCarte donnees() {
        Map<String, Long> compteurs = compterAgriculteurs();
        List<DelegationSurCarte> delegations = Arrays.stream(Delegation.values())
                .map(delegation -> new DelegationSurCarte(delegation, delegation.getLibelle(), delegation.getLatitude(),
                        delegation.getLongitude(),
                        compteurs.getOrDefault(cle(delegation.name(), StatutCompte.ACTIF.name()), 0L),
                        compteurs.getOrDefault(cle(delegation.name(), StatutCompte.EN_ATTENTE_VALIDATION.name()), 0L)))
                .toList();
        return new DonneesCarte(pressoirService.lister(null, false, PressoirService.Tri.NOM, Sort.Direction.ASC), delegations);
    }

    /**
     * Agrégation MongoDB :
     * { $match: { role: "AGRICULTEUR" } },
     * { $group: { _id: { delegation: "$agriculteur.delegation", statut: "$statut" }, nombre: { $sum: 1 } } }
     */
    private Map<String, Long> compterAgriculteurs() {
        Aggregation agregation = newAggregation(
                match(where("role").is(Role.AGRICULTEUR)),
                group("agriculteur.delegation", "statut").count().as("nombre"));
        Map<String, Long> compteurs = new HashMap<>();
        for (Document ligne : mongoTemplate.aggregate(agregation, Utilisateur.class, Document.class)) {
            // Chaque ligne : { _id: { delegation, statut }, nombre }
            Document groupe = ligne.get("_id", Document.class);
            compteurs.put(cle(groupe.getString("delegation"), groupe.getString("statut")),
                    ((Number) ligne.get("nombre")).longValue());
        }
        return compteurs;
    }

    private static String cle(String delegation, String statut) {
        return delegation + "/" + statut;
    }
}
