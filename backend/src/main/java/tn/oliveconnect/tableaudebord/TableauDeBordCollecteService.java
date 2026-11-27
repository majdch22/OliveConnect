package tn.oliveconnect.tableaudebord;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.alerte.AlertesCollecteService;
import tn.oliveconnect.alerte.StatutAlerte;
import tn.oliveconnect.alerte.TypeAlerte;
import tn.oliveconnect.demande.DemandeRecolte;
import tn.oliveconnect.demande.DemandesCollecteService;
import tn.oliveconnect.demande.StatutDemande;
import tn.oliveconnect.tournee.PlanificationService;
import tn.oliveconnect.tournee.StatutTournee;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.tournee.TourneeRepository;

/**
 * Tableau de bord du responsable de collecte : chiffres lus directement dans la base par des agrégations MongoDB.
 * - demandes et signalements par état ; signalements non résolus par type ;
 * - délai moyen de prise en charge d'un signalement ;
 * - tournées du jour par état ;
 * - charge des 7 prochains jours : quantité déjà planifiée et quantité validée qui attend une tournée ;
 * - capacité des pressoirs aujourd'hui (maximum, réservé, restant).
 * Les indicateurs de rendement et de qualité (pesées au pressoir) viendront avec la réception des bennes.
 */
@Service
@RequiredArgsConstructor
public class TableauDeBordCollecteService {

    private static final int JOURS_DE_CHARGE = 7;

    public record ChargeDuJour(LocalDate date, int planifieeKg, int enAttenteKg, long tournees) {
    }

    public record NombreParType(TypeAlerte type, String libelle, long nombre) {
    }

    public record TableauDeBord(Map<String, Long> demandesParStatut, Map<String, Long> alertesParStatut,
            List<NombreParType> alertesNonResoluesParType, Long delaiMoyenPriseEnChargeMinutes,
            Map<String, Long> tourneesDuJourParStatut, int quantitePlanifieeAujourdhuiKg, List<ChargeDuJour> charge,
            List<PlanificationService.PressoirDisponible> pressoirsAujourdhui) {
    }

    private final MongoTemplate mongoTemplate;
    private final DemandesCollecteService demandesService;
    private final AlertesCollecteService alertesService;
    private final PlanificationService planificationService;
    private final TourneeRepository tourneeRepository;
    private final Clock horloge;

    public TableauDeBord donnees() {
        LocalDate aujourdhui = LocalDate.now(horloge);
        Map<String, Long> tourneesDuJour = new HashMap<>();
        int planifieeAujourdhui = 0;
        for (Tournee tournee : tourneeRepository.findByDateOrderByNumeroAsc(aujourdhui)) {
            tourneesDuJour.merge(tournee.getStatut().name(), 1L, Long::sum);
            if (StatutTournee.ACTIVES.contains(tournee.getStatut())) {
                planifieeAujourdhui += tournee.quantiteEstimeeKg();
            }
        }
        return new TableauDeBord(demandesService.compteurs(), alertesService.compteurs(), alertesNonResoluesParType(),
                delaiMoyenPriseEnChargeMinutes(), tourneesDuJour, planifieeAujourdhui, charge(aujourdhui),
                planificationService.options(aujourdhui).pressoirs());
    }

    /** { $match: { statut: { $in: [non résolus] } } }, { $group: { _id: "$type", nombre: { $sum: 1 } } } */
    private List<NombreParType> alertesNonResoluesParType() {
        TypedAggregation<Alerte> agregation = newAggregation(Alerte.class,
                match(where("statut").in(StatutAlerte.NON_RESOLUS)), group("type").count().as("nombre"));
        Map<String, Long> nombres = new HashMap<>();
        for (Document ligne : mongoTemplate.aggregate(agregation, Document.class)) {
            nombres.put(ligne.getString("_id"), ((Number) ligne.get("nombre")).longValue());
        }
        List<NombreParType> resultat = new ArrayList<>();
        for (TypeAlerte type : TypeAlerte.values()) {
            resultat.add(new NombreParType(type, type.getLibelle(), nombres.getOrDefault(type.name(), 0L)));
        }
        return resultat;
    }

    /**
     * { $match: { datePriseEnCharge: { $exists: true } } },
     * { $project: { delai: { $subtract: ["$datePriseEnCharge", "$dateCreation"] } } },
     * { $group: { _id: null, moyenne: { $avg: "$delai" } } }   (en millisecondes)
     */
    private Long delaiMoyenPriseEnChargeMinutes() {
        TypedAggregation<Alerte> agregation = newAggregation(Alerte.class,
                match(where("datePriseEnCharge").exists(true).ne(null)),
                project().and(ArithmeticOperators.Subtract.valueOf("datePriseEnCharge").subtract("dateCreation")).as("delai"),
                group().avg("delai").as("moyenne"));
        Document resultat = mongoTemplate.aggregate(agregation, Document.class).getUniqueMappedResult();
        if (resultat == null || resultat.get("moyenne") == null) {
            return null;
        }
        return Math.round(((Number) resultat.get("moyenne")).doubleValue() / 60_000);
    }

    /**
     * Pour chacun des 7 prochains jours :
     * - planifiée : { $match: { date: [aujourd'hui, +7j], statut: actives } }, { $unwind: "$etapes" },
     *   { $group: { _id: "$date", kg: { $sum: "$etapes.quantiteEstimeeKg" }, tournees: { $addToSet: "$_id" } } }
     * - en attente : { $match: { statut: "VALIDEE", dateSouhaitee: [aujourd'hui, +7j] } },
     *   { $group: { _id: "$dateSouhaitee", kg: { $sum: "$quantiteEstimeeKg" } } }
     */
    private List<ChargeDuJour> charge(LocalDate aujourdhui) {
        LocalDate fin = aujourdhui.plusDays(JOURS_DE_CHARGE - 1);
        Map<LocalDate, Integer> planifiee = new HashMap<>();
        Map<LocalDate, Long> tournees = new HashMap<>();
        TypedAggregation<Tournee> agregationTournees = newAggregation(Tournee.class,
                match(where("date").gte(aujourdhui).lte(fin).and("statut").in(StatutTournee.ACTIVES)),
                unwind("etapes"),
                group("date").sum("etapes.quantiteEstimeeKg").as("kg").addToSet("_id").as("tournees"));
        for (Document ligne : mongoTemplate.aggregate(agregationTournees, Document.class)) {
            LocalDate jour = jour(ligne.get("_id"));
            planifiee.put(jour, ((Number) ligne.get("kg")).intValue());
            tournees.put(jour, (long) ligne.getList("tournees", Object.class).size());
        }
        Map<LocalDate, Integer> enAttente = new HashMap<>();
        TypedAggregation<DemandeRecolte> agregationDemandes = newAggregation(DemandeRecolte.class,
                match(where("statut").is(StatutDemande.VALIDEE).and("dateSouhaitee").gte(aujourdhui).lte(fin)),
                group("dateSouhaitee").sum("quantiteEstimeeKg").as("kg"));
        for (Document ligne : mongoTemplate.aggregate(agregationDemandes, Document.class)) {
            enAttente.put(jour(ligne.get("_id")), ((Number) ligne.get("kg")).intValue());
        }
        List<ChargeDuJour> charge = new ArrayList<>();
        for (LocalDate jour = aujourdhui; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            charge.add(new ChargeDuJour(jour, planifiee.getOrDefault(jour, 0), enAttente.getOrDefault(jour, 0),
                    tournees.getOrDefault(jour, 0L)));
        }
        return charge;
    }

    /** Les dates sont rangées par Spring Data à minuit dans le fuseau du serveur : on les relit dans le même fuseau. */
    private static LocalDate jour(Object valeur) {
        return ((Date) valeur).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
