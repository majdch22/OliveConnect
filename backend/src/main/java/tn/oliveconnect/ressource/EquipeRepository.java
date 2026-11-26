package tn.oliveconnect.ressource;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux équipes d'ouvriers. */
public interface EquipeRepository extends MongoRepository<Equipe, String> {

    Optional<Equipe> findByNomIgnoreCase(String nom);

    boolean existsByIdAndArchiveeFalse(String id);
}
