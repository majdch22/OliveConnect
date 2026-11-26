package tn.oliveconnect.verger;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux vergers. */
public interface VergerRepository extends MongoRepository<Verger, String> {

    List<Verger> findByAgriculteurIdOrderByNomAsc(String agriculteurId);

    boolean existsByAgriculteurIdAndNomIgnoreCaseAndIdNot(String agriculteurId, String nom, String id);

    long countByAgriculteurIdAndArchiveFalse(String agriculteurId);
}
