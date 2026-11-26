package tn.oliveconnect.referentiel;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Accès aux pressoirs. */
public interface PressoirRepository extends MongoRepository<Pressoir, String> {

    boolean existsByIdAndArchiveFalse(String id);
}
