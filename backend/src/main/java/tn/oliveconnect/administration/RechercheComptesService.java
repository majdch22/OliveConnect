package tn.oliveconnect.administration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Recherche de comptes avec filtres, tri et pagination, exécutée par MongoDB (et non dans le navigateur) :
 * seule la page demandée transite, même avec des centaines d'agriculteurs.
 * Les filtres rôle + statut s'appuient sur l'index composé « role_statut ».
 */
@Service
@RequiredArgsConstructor
public class RechercheComptesService {

    /**
     * Tri alphabétique à la française, sans tenir compte des majuscules ni des accents :
     * « chorfi », « Chorfi » et « Écrivain » sont rangés comme un lecteur l'attend.
     */
    private static final Collation ORDRE_ALPHABETIQUE_FRANCAIS =
            Collation.of(Locale.FRENCH).strength(Collation.ComparisonLevel.primary());

    private final MongoTemplate mongoTemplate;

    public Page<Utilisateur> rechercher(CriteresRecherche criteres) {
        List<Criteria> conditions = new ArrayList<>();
        if (!criteres.roles().isEmpty()) {
            conditions.add(where("role").in(criteres.roles()));
        }
        if (!criteres.statuts().isEmpty()) {
            conditions.add(where("statut").in(criteres.statuts()));
        }
        if (criteres.delegation() != null) {
            conditions.add(where("agriculteur.delegation").is(criteres.delegation()));
        }
        if (criteres.texte() != null && !criteres.texte().isBlank()) {
            conditions.add(rechercheTexte(criteres.texte().trim()));
        }

        Query requete = conditions.isEmpty() ? new Query() : new Query(new Criteria().andOperator(conditions));
        requete.collation(ORDRE_ALPHABETIQUE_FRANCAIS);
        PageRequest pagination = PageRequest.of(criteres.page(), criteres.taille(), ordre(criteres));
        List<Utilisateur> comptes = mongoTemplate.find(Query.of(requete).with(pagination), Utilisateur.class);

        // Le total n'est compté que s'il ne peut pas être déduit de la page elle-même
        return PageableExecutionUtils.getPage(comptes, pagination,
                () -> mongoTemplate.count(Query.of(requete).limit(-1).skip(-1), Utilisateur.class));
    }

    /**
     * Le texte saisi est cherché, sans tenir compte des majuscules, dans les principaux champs du compte.
     * Il est échappé pour qu'un caractère spécial (« + », « . ») soit cherché tel quel.
     */
    private Criteria rechercheTexte(String texte) {
        Pattern motif = Pattern.compile(Pattern.quote(texte), Pattern.CASE_INSENSITIVE);
        // Le téléphone est enregistré sans espaces : « 98 123 » doit trouver +21698123412
        Pattern chiffresTelephone = Pattern.compile(Pattern.quote(texte.replaceAll("\\s", "")));
        return new Criteria().orOperator(
                where("nom").regex(motif),
                where("prenom").regex(motif),
                where("email").regex(motif),
                where("telephone").regex(chiffresTelephone),
                where("agriculteur.cin").regex(chiffresTelephone),
                where("agriculteur.numeroAdherent").regex(motif));
    }

    /** Tri demandé, puis prénom et identifiant pour un ordre stable d'une page à l'autre. */
    private Sort ordre(CriteresRecherche criteres) {
        return Sort.by(criteres.sens(), criteres.tri().getChamp())
                .and(Sort.by(Sort.Direction.ASC, "prenom"))
                .and(Sort.by(Sort.Direction.ASC, "_id"));
    }
}
