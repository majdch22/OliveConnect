package tn.oliveconnect.sms;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;

/**
 * GET /api/administration/messages-envoyes?page=0   SMS simulés, des plus récents aux plus anciens (administrateur).
 */
@RestController
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class MessagesEnvoyesController {

    private final MessagesEnvoyesService messagesEnvoyesService;

    /** Un SMS tel qu'affiché dans la liste. */
    public record MessageReponse(String telephone, String texte, String libelleMotif, Instant dateEnvoi) {
    }

    @GetMapping("/api/administration/messages-envoyes")
    public PageResultat<MessageReponse> lister(@RequestParam(defaultValue = "0") int page) {
        return messagesEnvoyesService.lister(page);
    }
}
