package tn.oliveconnect.pressoir;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux capacités publiées par jour. */
public interface CapacitePressoirRepository extends MongoRepository<CapacitePressoir, String> {

    Optional<CapacitePressoir> findByPressoirIdAndDate(String pressoirId, LocalDate date);

    /** Capacités publiées de tous les pressoirs pour un jour (planification). */
    List<CapacitePressoir> findByDate(LocalDate date);
}
