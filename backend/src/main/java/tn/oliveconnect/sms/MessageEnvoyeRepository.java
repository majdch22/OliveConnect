package tn.oliveconnect.sms;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageEnvoyeRepository extends MongoRepository<MessageEnvoye, String> {

    Page<MessageEnvoye> findAllByOrderByDateEnvoiDesc(Pageable page);
}
