package tn.oliveconnect.pressoir;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux réceptions de bennes. */
public interface ReceptionRepository extends MongoRepository<Reception, String> {

    List<Reception> findByTourneeIdOrderByDateHeureAsc(String tourneeId);

    boolean existsByTourneeIdAndBenneId(String tourneeId, String benneId);
}
