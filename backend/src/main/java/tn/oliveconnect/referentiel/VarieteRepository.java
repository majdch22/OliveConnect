package tn.oliveconnect.referentiel;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux variétés d'olives. */
public interface VarieteRepository extends MongoRepository<Variete, String> {

    Optional<Variete> findByNomIgnoreCase(String nom);
}
