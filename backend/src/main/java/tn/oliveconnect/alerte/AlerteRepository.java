package tn.oliveconnect.alerte;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux signalements. */
public interface AlerteRepository extends MongoRepository<Alerte, String> {

    List<Alerte> findByAgriculteurIdOrderByDateCreationDesc(String agriculteurId);
}
