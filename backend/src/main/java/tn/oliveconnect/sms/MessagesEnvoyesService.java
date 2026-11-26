package tn.oliveconnect.sms;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;

/** Liste des SMS simulés pour l'administrateur, des plus récents aux plus anciens, par pages de 20. */
@Service
@RequiredArgsConstructor
public class MessagesEnvoyesService {

    private static final int TAILLE_PAGE = 20;

    private final MessageEnvoyeRepository messageRepository;

    public PageResultat<MessagesEnvoyesController.MessageReponse> lister(int page) {
        return PageResultat.depuis(messageRepository.findAllByOrderByDateEnvoiDesc(PageRequest.of(Math.max(page, 0), TAILLE_PAGE)),
                message -> new MessagesEnvoyesController.MessageReponse(message.telephone(), message.texte(),
                        message.motif().getLibelle(), message.dateEnvoi()));
    }
}
