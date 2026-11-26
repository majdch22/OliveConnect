package tn.oliveconnect.sms;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * SMS simulé : rien ne part vers un opérateur. Le message est écrit dans le journal du serveur et rangé dans
 * « messages_envoyes », que l'administrateur consulte depuis la supervision (c'est là qu'on lit un code en démonstration).
 */
@Service
@RequiredArgsConstructor
public class SmsSimule implements EnvoiSms {

    private static final Logger journal = LoggerFactory.getLogger(SmsSimule.class);

    private final MessageEnvoyeRepository messageRepository;
    private final Clock horloge;

    @Override
    public void envoyer(String telephone, String texte, MotifSms motif) {
        messageRepository.save(new MessageEnvoye(null, telephone, texte, motif, horloge.instant()));
        journal.info("SMS simulé vers {} ({}) : {}", telephone, motif.getLibelle(), texte);
    }
}
