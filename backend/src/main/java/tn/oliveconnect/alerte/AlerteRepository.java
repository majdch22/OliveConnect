package tn.oliveconnect.alerte;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux signalements. */
public interface AlerteRepository extends MongoRepository<Alerte, String> {

    List<Alerte> findByAgriculteurIdOrderByDateCreationDesc(String agriculteurId);

    /** Signalements pas encore résolus d'un agriculteur (compteur affiché sur chaque verger). */
    List<Alerte> findByAgriculteurIdAndStatutIn(String agriculteurId, Collection<StatutAlerte> statuts);

    /** Signalements pas encore résolus, tous agriculteurs confondus (carte publique). */
    List<Alerte> findByStatutIn(Collection<StatutAlerte> statuts);

    boolean existsByVergerId(String vergerId);
}
