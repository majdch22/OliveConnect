package tn.oliveconnect.notification;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;

/**
 * Notifications de la personne connectée (tous les rôles). Chacun ne voit que les siennes.
 * GET  /api/notifications?categorie=COLLECTE&nonLues=true&page=0
 * GET  /api/notifications/nombre-non-lues     pastille de la cloche
 * POST /api/notifications/{id}/lue
 * POST /api/notifications/tout-lire
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResultat<NotificationReponse> mesNotifications(@RequestParam(required = false) CategorieNotification categorie,
            @RequestParam(defaultValue = "false") boolean nonLues, @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal Jwt personne) {
        return notificationService.mesNotifications(personne.getSubject(), categorie, nonLues, page);
    }

    @GetMapping("/nombre-non-lues")
    public Map<String, Long> nombreNonLues(@AuthenticationPrincipal Jwt personne) {
        return Map.of("nombre", notificationService.nombreNonLues(personne.getSubject()));
    }

    @PostMapping("/{id}/lue")
    public NotificationReponse marquerLue(@PathVariable String id, @AuthenticationPrincipal Jwt personne) {
        return notificationService.marquerLue(id, personne.getSubject());
    }

    @PostMapping("/tout-lire")
    public Map<String, Long> toutLire(@AuthenticationPrincipal Jwt personne) {
        notificationService.toutMarquerLu(personne.getSubject());
        return Map.of("nombre", 0L);
    }
}
