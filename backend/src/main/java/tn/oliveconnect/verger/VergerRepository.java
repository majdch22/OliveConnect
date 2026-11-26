package tn.oliveconnect.verger;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux vergers. */
public interface VergerRepository extends MongoRepository<Verger, String> {

    List<Verger> findByAgriculteurIdOrderByNomAsc(String agriculteurId);

    /** Vergers visibles sur la carte publique. */
    List<Verger> findByArchiveFalseAndAgriculteurIdIn(Collection<String> agriculteurIds);

    boolean existsByAgriculteurIdAndNomIgnoreCaseAndIdNot(String agriculteurId, String nom, String id);

    long countByAgriculteurIdAndArchiveFalse(String agriculteurId);
}
