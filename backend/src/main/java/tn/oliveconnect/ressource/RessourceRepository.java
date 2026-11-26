package tn.oliveconnect.ressource;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux ressources. */
public interface RessourceRepository extends MongoRepository<Ressource, String> {

    long countByTypeAndArchiveeFalse(TypeRessource type);

    long countByTypeAndEtatAndArchiveeFalse(TypeRessource type, EtatRessource etat);

    long countByEquipeIdAndArchiveeFalse(String equipeId);

    long countByEquipeIdAndEtatAndArchiveeFalse(String equipeId, EtatRessource etat);
}
