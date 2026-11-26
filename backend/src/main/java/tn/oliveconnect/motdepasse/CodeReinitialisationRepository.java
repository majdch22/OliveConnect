package tn.oliveconnect.motdepasse;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CodeReinitialisationRepository extends MongoRepository<CodeReinitialisation, String> {

    Optional<CodeReinitialisation> findByUtilisateurId(String utilisateurId);

    void deleteByUtilisateurId(String utilisateurId);
}
