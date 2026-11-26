package tn.oliveconnect.demande;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux demandes de récolte. */
public interface DemandeRecolteRepository extends MongoRepository<DemandeRecolte, String> {

    List<DemandeRecolte> findByAgriculteurIdOrderByDateSouhaiteeDesc(String agriculteurId);

    /** Une demande active existe-t-elle déjà pour ce verger et cette date (en ignorant la demande modifiée) ? */
    boolean existsByVergerIdAndDateSouhaiteeAndStatutInAndIdNot(String vergerId, LocalDate date,
            Collection<StatutDemande> statuts, String id);

    boolean existsByVergerId(String vergerId);
}
