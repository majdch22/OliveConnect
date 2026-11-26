package tn.oliveconnect.compteur;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Distribue les numéros lisibles, sans doublon même si deux personnes en demandent un au même instant :
 * l'incrément est fait par MongoDB en une seule opération atomique (findAndModify + $inc).
 */
@Service
@RequiredArgsConstructor
public class CompteurService {

    /** Compteur des numéros d'adhérent (ADH-0001, ADH-0002…). */
    public static final String ADHERENT = "ADHERENT";

    /** Compteur des codes de variétés (VAR-01…) et de pressoirs (PR-01…). */
    public static final String VARIETE = "VARIETE";
    public static final String PRESSOIR = "PRESSOIR";

    private final MongoTemplate mongoTemplate;

    /** Renvoie la valeur suivante du compteur (1 au premier appel). */
    public long prochaineValeur(String nomCompteur) {
        Compteur compteur = mongoTemplate.findAndModify(
                new Query(where("_id").is(nomCompteur)),
                new Update().inc("valeur", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Compteur.class);
        return compteur.getValeur();
    }

    /** Fait repartir le compteur au moins de cette valeur (utile quand des numéros existent déjà en base). */
    public void garantirMinimum(String nomCompteur, long minimum) {
        mongoTemplate.upsert(new Query(where("_id").is(nomCompteur)), new Update().max("valeur", minimum), Compteur.class);
    }

    /** Prochain numéro d'adhérent, au format ADH-0042. */
    public String prochainNumeroAdherent() {
        return "ADH-%04d".formatted(prochaineValeur(ADHERENT));
    }

    /** Prochain code de variété, au format VAR-07. */
    public String prochainCodeVariete() {
        return "VAR-%02d".formatted(prochaineValeur(VARIETE));
    }

    /**
     * Prochain code d'un compteur, mis en forme avec le modèle donné.
     * Exemple : prochainCode("RESSOURCE_BENNE", "BN-%02d") donne BN-01, puis BN-02…
     */
    public String prochainCode(String nomCompteur, String modele) {
        return modele.formatted(prochaineValeur(nomCompteur));
    }

    /** Prochain code de pressoir, au format PR-05. */
    public String prochainCodePressoir() {
        return "PR-%02d".formatted(prochaineValeur(PRESSOIR));
    }
}
