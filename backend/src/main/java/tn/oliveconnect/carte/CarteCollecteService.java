package tn.oliveconnect.carte;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.alerte.StatutAlerte;
import tn.oliveconnect.alerte.TypeAlerte;
import tn.oliveconnect.demande.StatutDemande;
import tn.oliveconnect.referentiel.PressoirReponse;
import tn.oliveconnect.referentiel.PressoirService;
import tn.oliveconnect.tournee.StatutTournee;
import tn.oliveconnect.tournee.Tournee;
import tn.oliveconnect.tournee.TourneeRepository;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.StatutRecolte;
import tn.oliveconnect.verger.Verger;

/**
 * Carte du responsable de collecte : vergers (statut de récolte, maturité, demandes en cours), signalements non résolus,
 * pressoirs et vergers des tournées du jour.
 * Quand l'écran donne la zone visible, seuls les vergers de cette zone sont lus : requête MongoDB $geoWithin
 * sur l'index 2dsphere des vergers.
 */
@Service
@RequiredArgsConstructor
public class CarteCollecteService {

    public record VergerSurCarte(String id, String code, String nom, double latitude, double longitude, String nomAgriculteur,
            String nomVariete, double superficieHa, Integer niveauMaturite, StatutRecolte statutRecolte,
            String libelleStatutRecolte, long demandesEnCours, long signalementsNonResolus, String tourneeDuJour) {
    }

    public record AlerteSurCarte(String id, String numero, TypeAlerte type, String libelleType, StatutAlerte statut,
            String libelleStatut, double latitude, double longitude, String nomVerger, String description) {
    }

    public record DonneesCarteCollecte(List<VergerSurCarte> vergers, List<AlerteSurCarte> alertes,
            List<PressoirReponse> pressoirs) {
    }

    /** Zone visible de la carte (degrés). */
    public record Zone(double ouest, double sud, double est, double nord) {
    }

    private final MongoTemplate mongoTemplate;
    private final UtilisateurRepository utilisateurRepository;
    private final TourneeRepository tourneeRepository;
    private final PressoirService pressoirService;
    private final Clock horloge;

    public DonneesCarteCollecte donnees(Zone zone) {
        Criteria criteresVergers = where("archive").is(false);
        Criteria criteresAlertes = where("statut").in(StatutAlerte.NON_RESOLUS);
        if (zone != null) {
            GeoJsonPolygon rectangle = new GeoJsonPolygon(new Point(zone.ouest(), zone.sud()), new Point(zone.est(), zone.sud()),
                    new Point(zone.est(), zone.nord()), new Point(zone.ouest(), zone.nord()), new Point(zone.ouest(), zone.sud()));
            criteresVergers.and("localisation").within(rectangle);
            criteresAlertes.and("position").within(rectangle);
        }
        List<Verger> vergers = mongoTemplate.find(Query.query(criteresVergers).with(Sort.by("nom")), Verger.class);
        List<Alerte> alertes = mongoTemplate.find(Query.query(criteresAlertes).with(Sort.by(Sort.Direction.DESC, "dateCreation")),
                Alerte.class);

        Map<String, String> agriculteurs = utilisateurRepository.findAllById(
                        vergers.stream().map(Verger::getAgriculteurId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Utilisateur::getNomComplet));
        Map<String, Long> demandes = compterParVerger("demandes_recolte",
                where("statut").in(StatutDemande.ACTIFS.stream().map(Enum::name).toList()));
        Map<String, Long> signalements = compterParVerger("alertes",
                where("statut").in(StatutAlerte.NON_RESOLUS.stream().map(Enum::name).toList()));
        Map<String, String> tourneesDuJour = new HashMap<>();
        for (Tournee tournee : tourneeRepository.findByDateAndStatutIn(LocalDate.now(horloge), StatutTournee.ACTIVES)) {
            tournee.getEtapes().forEach(etape -> tourneesDuJour.put(etape.vergerId(), tournee.getNumero()));
        }

        List<VergerSurCarte> vergersSurCarte = vergers.stream().map(verger -> new VergerSurCarte(verger.getId(), verger.getCode(),
                verger.getNom(), verger.getLocalisation().getY(), verger.getLocalisation().getX(),
                agriculteurs.getOrDefault(verger.getAgriculteurId(), ""), verger.getNomVariete(), verger.getSuperficieHa(),
                verger.getNiveauMaturite(), verger.getStatutRecolte(), verger.getStatutRecolte().getLibelle(),
                demandes.getOrDefault(verger.getId(), 0L), signalements.getOrDefault(verger.getId(), 0L),
                tourneesDuJour.get(verger.getId()))).toList();
        List<AlerteSurCarte> alertesSurCarte = alertes.stream().map(alerte -> new AlerteSurCarte(alerte.getId(), alerte.getNumero(),
                alerte.getType(), alerte.getType().getLibelle(), alerte.getStatut(), alerte.getStatut().getLibelle(),
                alerte.getPosition().getY(), alerte.getPosition().getX(), alerte.getNomVerger(), alerte.getDescription())).toList();
        return new DonneesCarteCollecte(vergersSurCarte, alertesSurCarte,
                pressoirService.lister(null, false, PressoirService.Tri.NOM, Sort.Direction.ASC));
    }

    /** Agrégation : { $match: <critères> }, { $group: { _id: "$vergerId", nombre: { $sum: 1 } } }. */
    private Map<String, Long> compterParVerger(String collection, Criteria criteres) {
        Map<String, Long> compteurs = new HashMap<>();
        for (Document ligne : mongoTemplate.aggregate(newAggregation(match(criteres), group("vergerId").count().as("nombre")),
                collection, Document.class)) {
            compteurs.put(ligne.getString("_id"), ((Number) ligne.get("nombre")).longValue());
        }
        return compteurs;
    }
}
