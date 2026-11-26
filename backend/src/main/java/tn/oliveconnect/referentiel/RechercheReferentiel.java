package tn.oliveconnect.referentiel;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Construction des requêtes de liste des référentiels : recherche par nom ou code, archives masquées ou non,
 * tri alphabétique à la française. Ces listes sont courtes : elles ne sont pas paginées.
 */
final class RechercheReferentiel {

    private static final Collation ORDRE_ALPHABETIQUE_FRANCAIS =
            Collation.of(Locale.FRENCH).strength(Collation.ComparisonLevel.primary());

    private RechercheReferentiel() {
    }

    /**
     * @param texte           recherche dans le nom et le code (vide : pas de recherche)
     * @param champArchive    nom du champ booléen d'archivage
     * @param avecArchives    vrai pour inclure les éléments archivés
     * @param champTri        champ de tri
     * @param sens            sens du tri
     */
    static Query requete(String texte, String champArchive, boolean avecArchives, String champTri, Sort.Direction sens) {
        List<Criteria> conditions = new ArrayList<>();
        if (!avecArchives) {
            conditions.add(where(champArchive).is(false));
        }
        if (texte != null && !texte.isBlank()) {
            Pattern motif = Pattern.compile(Pattern.quote(texte.trim()), Pattern.CASE_INSENSITIVE);
            conditions.add(new Criteria().orOperator(where("nom").regex(motif), where("code").regex(motif)));
        }
        Query requete = conditions.isEmpty() ? new Query() : new Query(new Criteria().andOperator(conditions));
        return requete
                .with(Sort.by(sens, champTri).and(Sort.by("nom")))
                .collation(ORDRE_ALPHABETIQUE_FRANCAIS);
    }
}
