package tn.oliveconnect.compteur;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Au démarrage, place le compteur des adhérents au-dessus du plus grand numéro déjà attribué
 * (par exemple des numéros importés ou des comptes de démonstration), pour ne jamais donner deux fois le même numéro.
 * S'exécute après la création des comptes de démonstration.
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class AlignementCompteurAdherents implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;
    private final CompteurService compteurService;

    @Override
    public void run(ApplicationArguments arguments) {
        // Les numéros ont tous le format ADH-0000 : l'ordre alphabétique décroissant donne le plus grand
        Query plusGrandNumero = new Query(where("agriculteur.numeroAdherent").regex("^ADH-\\d+$"))
                .with(Sort.by(Sort.Direction.DESC, "agriculteur.numeroAdherent"))
                .limit(1);
        Utilisateur dernier = mongoTemplate.findOne(plusGrandNumero, Utilisateur.class);
        if (dernier != null) {
            long valeur = Long.parseLong(dernier.getAgriculteur().getNumeroAdherent().substring(4));
            compteurService.garantirMinimum(CompteurService.ADHERENT, valeur);
        }
    }
}
