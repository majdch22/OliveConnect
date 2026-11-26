package tn.oliveconnect.notification;

import java.time.Clock;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.PageResultat;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.sms.EnvoiSms;
import tn.oliveconnect.sms.MotifSms;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Prévient une personne, ou toutes les personnes actives d'un rôle (décision 13 : jamais une diffusion à tous).
 * - La notification est enregistrée (liste de la cloche) puis poussée en temps réel sur la file personnelle
 *   « /user/queue/notifications » de la personne, si elle est connectée.
 * - Un SMS part en plus si le contenu en prévoit un, si les SMS sont activés dans les paramètres
 *   et si la personne les accepte dans son profil.
 * Les autres espaces (collecte, chef d'équipe, pressoir) utilisent les mêmes méthodes notifierPersonne et notifierRole.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** File personnelle sur laquelle chaque navigateur connecté écoute ses notifications. */
    public static final String FILE_PERSONNELLE = "/queue/notifications";

    private static final int TAILLE_PAGE = 20;

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate envoiTempsReel;
    private final ParametresService parametresService;
    private final EnvoiSms envoiSms;
    private final Clock horloge;

    // --- Envoi ---

    public void notifierPersonne(String destinataireId, NouvelleNotification contenu) {
        utilisateurRepository.findById(destinataireId).ifPresent(destinataire -> notifier(destinataire, contenu));
    }

    public void notifierRole(Role role, NouvelleNotification contenu) {
        utilisateurRepository.findByRoleAndStatut(role, StatutCompte.ACTIF).forEach(destinataire -> notifier(destinataire, contenu));
    }

    /**
     * SMS seul, pour une personne qui ne peut pas se connecter (inscription refusée) ou qui doit le recevoir
     * quels que soient ses choix (code de mot de passe oublié).
     */
    public void envoyerSmsSeul(Utilisateur destinataire, String texte, MotifSms motif) {
        envoiSms.envoyer(destinataire.getTelephone(), texte, motif);
    }

    private void notifier(Utilisateur destinataire, NouvelleNotification contenu) {
        if (destinataire.preferences().application()) {
            Notification notification = new Notification();
            notification.setDestinataireId(destinataire.getId());
            notification.setCategorie(contenu.categorie());
            notification.setTitre(contenu.titre());
            notification.setMessage(contenu.message());
            notification.setLien(contenu.lien());
            notification.setPrioritaire(contenu.prioritaire());
            notification.setDateCreation(horloge.instant());
            NotificationReponse envoyee = NotificationReponse.depuis(notificationRepository.save(notification));
            // Le nom de la personne pour le courtier de messages est l'identifiant de son compte
            envoiTempsReel.convertAndSendToUser(destinataire.getId(), FILE_PERSONNELLE, envoyee);
        }
        boolean smsActives = parametresService.actuels().notifications().smsAgriculteurs();
        if (contenu.texteSms() != null && smsActives && destinataire.preferences().sms()) {
            envoiSms.envoyer(destinataire.getTelephone(), contenu.texteSms(), MotifSms.COLLECTE);
        }
    }

    // --- Lecture ---

    /**
     * Notifications de la personne, les plus récentes d'abord.
     *
     * @param categorie une seule famille, ou null pour toutes
     * @param nonLues   vrai pour ne garder que les non lues
     */
    public PageResultat<NotificationReponse> mesNotifications(String destinataireId, CategorieNotification categorie,
            boolean nonLues, int page) {
        Criteria criteres = Criteria.where("destinataireId").is(destinataireId);
        if (categorie != null) {
            criteres.and("categorie").is(categorie);
        }
        if (nonLues) {
            criteres.and("lue").is(false);
        }
        PageRequest pagination = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "dateCreation"));
        Query requete = new Query(criteres).with(pagination);
        var elements = mongoTemplate.find(requete, Notification.class);
        var resultat = PageableExecutionUtils.getPage(elements, pagination,
                () -> mongoTemplate.count(Query.of(requete).limit(-1).skip(-1), Notification.class));
        return PageResultat.depuis(resultat, NotificationReponse::depuis);
    }

    public long nombreNonLues(String destinataireId) {
        return notificationRepository.countByDestinataireIdAndLueFalse(destinataireId);
    }

    /** Une notification d'une autre personne est traitée comme inexistante (404), sans en dire plus. */
    public NotificationReponse marquerLue(String id, String destinataireId) {
        Notification notification = notificationRepository.findById(id)
                .filter(trouvee -> trouvee.getDestinataireId().equals(destinataireId))
                .orElseThrow(() -> new ErreurMetierException(HttpStatus.NOT_FOUND, "NOTIFICATION_INTROUVABLE",
                        "Cette notification n'existe pas."));
        notification.setLue(true);
        return NotificationReponse.depuis(notificationRepository.save(notification));
    }

    /** « Tout marquer lu » : une seule requête pour toutes les notifications non lues de la personne. */
    public void toutMarquerLu(String destinataireId) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("destinataireId").is(destinataireId).and("lue").is(false)),
                Update.update("lue", true), Notification.class);
    }
}
