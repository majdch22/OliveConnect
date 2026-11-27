package tn.oliveconnect.tournee;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux tournées. */
public interface TourneeRepository extends MongoRepository<Tournee, String> {

    List<Tournee> findByDateOrderByNumeroAsc(LocalDate date);

    /** Tournées du jour qui occupent déjà leurs ressources et leur chef d'équipe. */
    List<Tournee> findByDateAndStatutIn(LocalDate date, Collection<StatutTournee> statuts);
}
