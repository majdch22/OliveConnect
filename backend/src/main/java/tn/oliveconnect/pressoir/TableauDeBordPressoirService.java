package tn.oliveconnect.pressoir;

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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.referentiel.CategorieQualite;
import tn.oliveconnect.referentiel.Pressoir;

/**
 * Tableau de bord du responsable du pressoir (UC52), calculé par des agrégations MongoDB sur « receptions » :
 * volume reçu, nombre de bennes, remplissage moyen des bennes, acidité moyenne pondérée par le poids,
 * répartition par catégorie de qualité, volume par jour et qualité par variété (parts de chaque benne reliées aux vergers).
 */
@Service
@RequiredArgsConstructor
public class TableauDeBordPressoirService {

    public record Totaux(int poidsKg, long bennes, Integer remplissageMoyen, Double aciditeMoyenne) {
    }

    public record ParCategorie(CategorieQualite categorie, String libelle, int poidsKg, long bennes) {
    }

    public record ParJour(LocalDate date, int poidsKg, long bennes) {
    }

    public record ParVariete(String variete, int poidsKg, Double aciditeMoyenne) {
    }

    public record TableauDeBord(int jours, Totaux totaux, CapaciteService.CapaciteDuJour aujourdhui, List<ParJour> parJour,
            List<ParCategorie> parCategorie, List<ParVariete> parVariete) {
    }

    private final MongoTemplate mongoTemplate;
    private final CapaciteService capaciteService;
    private final Clock horloge;

    public TableauDeBord donnees(Pressoir pressoir, int jours) {
        int duree = Math.clamp(jours, 1, 366);
        LocalDate fin = LocalDate.now(horloge);
        LocalDate debut = fin.minusDays(duree - 1L);
        AggregationOperation periode = Aggregation.match(where("pressoirId").is(pressoir.getId())
                .and("jour").gte(debut).lte(fin));
        return new TableauDeBord(duree, totaux(periode), capaciteService.duJour(pressoir, fin), parJour(periode, debut, fin),
                parCategorie(periode), parVariete(periode));
    }

    /**
     * { $group: { _id: null, poids: { $sum: "$poidsNetKg" }, bennes: { $sum: 1 }, remplissage: { $avg: "$tauxRemplissage" },
     *             aciditePonderee: { $sum: { $multiply: ["$poidsNetKg", "$aciditePourcent"] } } } }
     */
    private Totaux totaux(AggregationOperation periode) {
        Document resultat = premier(Aggregation.newAggregation(periode, etape("$group", new Document("_id", null)
                .append("poids", new Document("$sum", "$poidsNetKg"))
                .append("bennes", new Document("$sum", 1))
                .append("remplissage", new Document("$avg", "$tauxRemplissage"))
                .append("aciditePonderee", new Document("$sum", new Document("$multiply", List.of("$poidsNetKg", "$aciditePourcent")))))));
        if (resultat == null) {
            return new Totaux(0, 0, null, null);
        }
        int poids = nombre(resultat, "poids").intValue();
        return new Totaux(poids, nombre(resultat, "bennes").longValue(),
                (int) Math.round(nombre(resultat, "remplissage").doubleValue()),
                poids == 0 ? null : arrondi(nombre(resultat, "aciditePonderee").doubleValue() / poids));
    }

    /** { $group: { _id: "$jour", poids: { $sum: "$poidsNetKg" }, bennes: { $sum: 1 } } } — les jours sans réception valent 0. */
    private List<ParJour> parJour(AggregationOperation periode, LocalDate debut, LocalDate fin) {
        Map<LocalDate, Document> lignes = new HashMap<>();
        for (Document ligne : resultats(Aggregation.newAggregation(periode, etape("$group", new Document("_id", "$jour")
                .append("poids", new Document("$sum", "$poidsNetKg")).append("bennes", new Document("$sum", 1)))))) {
            lignes.put(((Date) ligne.get("_id")).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), ligne);
        }
        List<ParJour> jours = new ArrayList<>();
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            Document ligne = lignes.get(jour);
            jours.add(ligne == null ? new ParJour(jour, 0, 0)
                    : new ParJour(jour, nombre(ligne, "poids").intValue(), nombre(ligne, "bennes").longValue()));
        }
        return jours;
    }

    /** { $group: { _id: "$categorie", poids: { $sum: "$poidsNetKg" }, bennes: { $sum: 1 } } } — toutes les catégories, même vides. */
    private List<ParCategorie> parCategorie(AggregationOperation periode) {
        Map<String, Document> lignes = new HashMap<>();
        for (Document ligne : resultats(Aggregation.newAggregation(periode, etape("$group", new Document("_id", "$categorie")
                .append("poids", new Document("$sum", "$poidsNetKg")).append("bennes", new Document("$sum", 1)))))) {
            lignes.put(ligne.getString("_id"), ligne);
        }
        List<ParCategorie> categories = new ArrayList<>();
        for (CategorieQualite categorie : CategorieQualite.values()) {
            Document ligne = lignes.get(categorie.name());
            categories.add(new ParCategorie(categorie, categorie.getLibelle(), ligne == null ? 0 : nombre(ligne, "poids").intValue(),
                    ligne == null ? 0 : nombre(ligne, "bennes").longValue()));
        }
        return categories;
    }

    /**
     * Qualité par variété : chaque part de benne est reliée à son verger pour connaître la variété.
     * { $unwind: "$parts" }, { $addFields: { vergerObjet: { $toObjectId: "$parts.vergerId" } } },
     * { $lookup: { from: "vergers", localField: "vergerObjet", foreignField: "_id", as: "verger" } }, { $unwind: "$verger" },
     * { $group: { _id: "$verger.nomVariete", poids: { $sum: "$parts.poidsKg" },
     *             aciditePonderee: { $sum: { $multiply: ["$parts.poidsKg", "$aciditePourcent"] } } } }, { $sort: { poids: -1 } }
     */
    private List<ParVariete> parVariete(AggregationOperation periode) {
        List<ParVariete> varietes = new ArrayList<>();
        for (Document ligne : resultats(Aggregation.newAggregation(periode,
                etape("$unwind", "$parts"),
                etape("$addFields", new Document("vergerObjet", new Document("$toObjectId", "$parts.vergerId"))),
                etape("$lookup", new Document("from", "vergers").append("localField", "vergerObjet")
                        .append("foreignField", "_id").append("as", "verger")),
                etape("$unwind", "$verger"),
                etape("$group", new Document("_id", "$verger.nomVariete")
                        .append("poids", new Document("$sum", "$parts.poidsKg"))
                        .append("aciditePonderee", new Document("$sum", new Document("$multiply", List.of("$parts.poidsKg", "$aciditePourcent"))))),
                etape("$sort", new Document("poids", -1))))) {
            int poids = nombre(ligne, "poids").intValue();
            varietes.add(new ParVariete(ligne.getString("_id"), poids,
                    poids == 0 ? null : arrondi(nombre(ligne, "aciditePonderee").doubleValue() / poids)));
        }
        return varietes;
    }

    // --- Outils ---

    private static AggregationOperation etape(String operateur, Object contenu) {
        return contexte -> new Document(operateur, contenu);
    }

    private List<Document> resultats(Aggregation agregation) {
        return mongoTemplate.aggregate(agregation, "receptions", Document.class).getMappedResults();
    }

    private Document premier(Aggregation agregation) {
        List<Document> lignes = resultats(agregation);
        return lignes.isEmpty() ? null : lignes.getFirst();
    }

    private static Number nombre(Document ligne, String cle) {
        Object valeur = ligne.get(cle);
        return valeur == null ? 0 : (Number) valeur;
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 100) / 100.0;
    }
}
