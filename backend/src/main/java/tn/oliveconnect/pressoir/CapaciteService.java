package tn.oliveconnect.pressoir;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.referentiel.Pressoir;
import tn.oliveconnect.tournee.StatutTournee;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Capacité d'un pressoir jour par jour (UC51, décision 7) :
 * - maximum : la capacité publiée par le responsable pour ce jour, sinon la capacité habituelle du pressoir ;
 * - engagé : pour chaque tournée du jour vers ce pressoir, le plus grand de sa quantité estimée et de ce qui en a
 *   déjà été reçu (seulement le reçu une fois la tournée clôturée) ;
 * - reçu : somme des pesées du jour (agrégation sur « receptions ») ; attendu = engagé − reçu ;
 * - restant : maximum − engagé.
 * Le responsable peut publier la capacité d'aujourd'hui ou d'un jour à venir, avec un motif facultatif.
 */
@Service
@RequiredArgsConstructor
public class CapaciteService {

    /** Capacité d'un jour, telle qu'affichée au responsable. */
    public record CapaciteDuJour(LocalDate date, int capaciteHabituelleKg, int maximumKg, boolean publiee, String motif,
            String nomAuteur, int attenduKg, int recuKg, int restantKg) {
    }

    private final CapacitePressoirRepository capaciteRepository;
    private final MongoTemplate mongoTemplate;
    private final Clock horloge;

    public CapaciteDuJour duJour(Pressoir pressoir, LocalDate date) {
        return periode(pressoir, date, date).getFirst();
    }

    /** Capacité de chaque jour de la période (bornes comprises). */
    public List<CapaciteDuJour> periode(Pressoir pressoir, LocalDate debut, LocalDate fin) {
        Map<LocalDate, CapacitePressoir> publiees = mongoTemplate.find(Query.query(where("pressoirId").is(pressoir.getId())
                        .and("date").gte(debut).lte(fin)), CapacitePressoir.class).stream()
                .collect(Collectors.toMap(CapacitePressoir::getDate, Function.identity()));
        Map<LocalDate, Integer> recuParJour = recuParJour(pressoir.getId(), debut, fin);
        Map<String, Integer> recuParTournee = recuParTournee(pressoir.getId(), debut, fin);
        Map<LocalDate, Integer> engageParJour = new HashMap<>();
        for (Tournee tournee : mongoTemplate.find(Query.query(where("pressoirId").is(pressoir.getId())
                .and("date").gte(debut).lte(fin).and("statut").in(StatutTournee.ACTIVES)), Tournee.class)) {
            int recu = recuParTournee.getOrDefault(tournee.getId(), 0);
            int engage = tournee.getStatut() == StatutTournee.CLOTUREE ? recu : Math.max(tournee.quantiteEstimeeKg(), recu);
            engageParJour.merge(tournee.getDate(), engage, Integer::sum);
        }
        List<CapaciteDuJour> jours = new ArrayList<>();
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            CapacitePressoir publiee = publiees.get(jour);
            int maximum = publiee == null ? pressoir.getCapaciteJournaliereKg() : publiee.getMaximumKg();
            int recu = recuParJour.getOrDefault(jour, 0);
            // Pesées hors tournée du jour (benne arrivée le lendemain…) : comptées aussi dans l'engagé
            int engage = Math.max(engageParJour.getOrDefault(jour, 0), recu);
            jours.add(new CapaciteDuJour(jour, pressoir.getCapaciteJournaliereKg(), maximum, publiee != null,
                    publiee == null ? null : publiee.getMotif(), publiee == null ? null : publiee.getNomAuteur(),
                    Math.max(engage - recu, 0), recu, maximum - engage));
        }
        return jours;
    }

    public CapaciteDuJour publier(Pressoir pressoir, LocalDate date, int maximumKg, String motif, Utilisateur auteur) {
        if (date.isBefore(LocalDate.now(horloge))) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "La capacité d'un jour passé ne peut plus être changée.", Map.of("date", "Choisissez aujourd'hui ou un jour à venir."));
        }
        CapacitePressoir capacite = capaciteRepository.findByPressoirIdAndDate(pressoir.getId(), date).orElseGet(() -> {
            CapacitePressoir nouvelle = new CapacitePressoir();
            nouvelle.setPressoirId(pressoir.getId());
            nouvelle.setDate(date);
            return nouvelle;
        });
        capacite.setMaximumKg(maximumKg);
        capacite.setMotif(motif == null || motif.isBlank() ? null : motif.trim());
        capacite.setAuteurId(auteur.getId());
        capacite.setNomAuteur(auteur.getNomComplet());
        capacite.setDateModification(horloge.instant());
        capaciteRepository.save(capacite);
        return duJour(pressoir, date);
    }

    /** { $match: { pressoirId, jour: [début, fin] } }, { $group: { _id: "$jour", recu: { $sum: "$poidsNetKg" } } } */
    private Map<LocalDate, Integer> recuParJour(String pressoirId, LocalDate debut, LocalDate fin) {
        TypedAggregation<Reception> agregation = newAggregation(Reception.class,
                match(where("pressoirId").is(pressoirId).and("jour").gte(debut).lte(fin)),
                group("jour").sum("poidsNetKg").as("recu"));
        Map<LocalDate, Integer> recu = new HashMap<>();
        for (Document ligne : mongoTemplate.aggregate(agregation, Document.class)) {
            recu.put(((Date) ligne.get("_id")).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    ((Number) ligne.get("recu")).intValue());
        }
        return recu;
    }

    /** Même agrégation, regroupée par tournée. */
    private Map<String, Integer> recuParTournee(String pressoirId, LocalDate debut, LocalDate fin) {
        TypedAggregation<Reception> agregation = newAggregation(Reception.class,
                match(where("pressoirId").is(pressoirId).and("jour").gte(debut.minusDays(1)).lte(fin.plusDays(1))),
                group("tourneeId").sum("poidsNetKg").as("recu"));
        Map<String, Integer> recu = new HashMap<>();
        for (Document ligne : mongoTemplate.aggregate(agregation, Document.class)) {
            recu.put(ligne.getString("_id"), ((Number) ligne.get("recu")).intValue());
        }
        return recu;
    }
}
