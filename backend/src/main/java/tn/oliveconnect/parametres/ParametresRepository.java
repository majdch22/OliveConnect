package tn.oliveconnect.parametres;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès au document unique des paramètres. */
public interface ParametresRepository extends MongoRepository<Parametres, String> {
}
