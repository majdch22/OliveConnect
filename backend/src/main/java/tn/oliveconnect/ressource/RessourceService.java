package tn.oliveconnect.ressource;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;
import tn.oliveconnect.utilisateur.Identifiants;

/**
 * Gestion des ressources (tracteurs, bennes, ouvriers, matériel).
 *
 * Règles selon le type :
 * - une benne a une capacité (entre 500 et 20 000 kg) ;
 * - un matériel a une quantité (au moins 1) ;
 * - seul un ouvrier peut appartenir à une équipe, et l'équipe doit exister et ne pas être archivée ;
 * - le type ne change pas après la création (le code en dépend).
 * Une ressource n'est jamais supprimée : elle est archivée.
 */
@Service
@RequiredArgsConstructor
public class RessourceService {

    private static final Collation ORDRE_ALPHABETIQUE_FRANCAIS =
            Collation.of(Locale.FRENCH).strength(Collation.ComparisonLevel.primary());

    /** Colonnes de tri proposées. */
    public enum Tri {
        CODE("code"), NOM("nom"), TYPE("type"), ETAT("etat"), CAPACITE("capaciteKg");

        private final String champ;

        Tri(String champ) {
            this.champ = champ;
        }
    }

    /** Nombre de ressources actives et disponibles, pour un type. */
    public record ResumeParType(TypeRessource type, String libelle, long total, long disponibles) {
    }

    private final RessourceRepository ressourceRepository;
    private final EquipeRepository equipeRepository;
    private final CompteurService compteurService;
    private final MongoTemplate mongoTemplate;

    public PageResultat<RessourceReponse> lister(String texte, TypeRessource type, EtatRessource etat, String equipeId,
            boolean avecArchives, Tri tri, Sort.Direction sens, int page, int taille) {
        List<Criteria> conditions = new ArrayList<>();
        if (!avecArchives) {
            conditions.add(where("archivee").is(false));
        }
        if (type != null) {
            conditions.add(where("type").is(type));
        }
        if (etat != null) {
            conditions.add(where("etat").is(etat));
        }
        if (equipeId != null && !equipeId.isBlank()) {
            conditions.add(where("equipeId").is(equipeId));
        }
        if (texte != null && !texte.isBlank()) {
            Pattern motif = Pattern.compile(Pattern.quote(texte.trim()), Pattern.CASE_INSENSITIVE);
            conditions.add(new Criteria().orOperator(where("nom").regex(motif), where("code").regex(motif),
                    where("matricule").regex(motif)));
        }
        Query requete = conditions.isEmpty() ? new Query() : new Query(new Criteria().andOperator(conditions));
        requete.collation(ORDRE_ALPHABETIQUE_FRANCAIS);
        PageRequest pagination = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, 100),
                Sort.by(sens, tri.champ).and(Sort.by("code")));

        List<Ressource> ressources = mongoTemplate.find(Query.of(requete).with(pagination), Ressource.class);
        Map<String, String> nomsEquipes = nomsDesEquipes();
        return PageResultat.depuis(
                PageableExecutionUtils.getPage(ressources, pagination,
                        () -> mongoTemplate.count(Query.of(requete).limit(-1).skip(-1), Ressource.class)),
                ressource -> RessourceReponse.depuis(ressource, nomsEquipes.get(ressource.getEquipeId())));
    }

    public List<ResumeParType> resume() {
        return Arrays.stream(TypeRessource.values())
                .map(type -> new ResumeParType(type, type.getLibelle(),
                        ressourceRepository.countByTypeAndArchiveeFalse(type),
                        ressourceRepository.countByTypeAndEtatAndArchiveeFalse(type, EtatRessource.DISPONIBLE)))
                .toList();
    }

    public RessourceReponse creer(RessourceRequete requete) {
        verifierSelonLeType(requete);
        Ressource ressource = new Ressource();
        ressource.setType(requete.type());
        ressource.setCode(compteurService.prochainCode("RESSOURCE_" + requete.type().name(), requete.type().getFormatCode()));
        remplir(ressource, requete);
        return reponse(ressourceRepository.save(ressource));
    }

    public RessourceReponse modifier(String id, RessourceRequete requete) {
        Ressource ressource = trouver(id);
        if (ressource.getType() != requete.type()) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.",
                    Map.of("type", "Le type d'une ressource ne change pas après sa création."));
        }
        verifierSelonLeType(requete);
        remplir(ressource, requete);
        return reponse(ressourceRepository.save(ressource));
    }

    public RessourceReponse changerArchivage(String id, boolean archivee) {
        Ressource ressource = trouver(id);
        ressource.setArchivee(archivee);
        return reponse(ressourceRepository.save(ressource));
    }

    /** Les champs propres à chaque type, avec un message par champ en cas d'erreur. */
    private void verifierSelonLeType(RessourceRequete requete) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        if (requete.type() == TypeRessource.BENNE
                && (requete.capaciteKg() == null || requete.capaciteKg() < 500 || requete.capaciteKg() > 20_000)) {
            erreurs.put("capaciteKg", "La capacité d'une benne est comprise entre 500 et 20 000 kg.");
        }
        if (requete.type() == TypeRessource.MATERIEL && (requete.quantite() == null || requete.quantite() < 1)) {
            erreurs.put("quantite", "Indiquez la quantité disponible (au moins 1).");
        }
        boolean aUneEquipe = requete.equipeId() != null && !requete.equipeId().isBlank();
        if (aUneEquipe && requete.type() != TypeRessource.OUVRIER) {
            erreurs.put("equipeId", "Seul un ouvrier peut appartenir à une équipe.");
        } else if (aUneEquipe && !equipeRepository.existsByIdAndArchiveeFalse(requete.equipeId())) {
            erreurs.put("equipeId", "Cette équipe n'existe pas ou est archivée.");
        }
        if (!erreurs.isEmpty()) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.", erreurs);
        }
    }

    /** Ne garde que les champs qui ont un sens pour le type de la ressource. */
    private void remplir(Ressource ressource, RessourceRequete requete) {
        TypeRessource type = requete.type();
        boolean estVehicule = type == TypeRessource.TRACTEUR || type == TypeRessource.BENNE;
        ressource.setNom(requete.nom().trim());
        ressource.setEtat(requete.etat());
        ressource.setMatricule(estVehicule && requete.matricule() != null && !requete.matricule().isBlank()
                ? requete.matricule().trim().toUpperCase() : null);
        ressource.setCapaciteKg(type == TypeRessource.BENNE ? requete.capaciteKg() : null);
        ressource.setQuantite(type == TypeRessource.MATERIEL ? requete.quantite() : null);
        ressource.setEquipeId(type == TypeRessource.OUVRIER && requete.equipeId() != null && !requete.equipeId().isBlank()
                ? requete.equipeId() : null);
        ressource.setTelephone(type == TypeRessource.OUVRIER && requete.telephone() != null && !requete.telephone().isBlank()
                ? Identifiants.normaliserTelephone(requete.telephone()) : null);
    }

    private RessourceReponse reponse(Ressource ressource) {
        String nomEquipe = ressource.getEquipeId() == null ? null
                : equipeRepository.findById(ressource.getEquipeId()).map(Equipe::getNom).orElse(null);
        return RessourceReponse.depuis(ressource, nomEquipe);
    }

    private Map<String, String> nomsDesEquipes() {
        return equipeRepository.findAll().stream().collect(Collectors.toMap(Equipe::getId, Equipe::getNom, (a, b) -> a));
    }

    private Ressource trouver(String id) {
        return ressourceRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE", "Cette ressource n'existe pas."));
    }

    /** Ouvriers actifs d'une équipe, par ordre alphabétique. */
    public List<RessourceReponse> membres(String equipeId) {
        Query requete = new Query(where("equipeId").is(equipeId).and("archivee").is(false))
                .with(Sort.by("nom")).collation(ORDRE_ALPHABETIQUE_FRANCAIS);
        return mongoTemplate.find(requete, Ressource.class).stream().map(this::reponse).toList();
    }
}
