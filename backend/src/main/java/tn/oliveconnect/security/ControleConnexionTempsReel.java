package tn.oliveconnect.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Contrôle chaque message STOMP reçu d'un navigateur.
 * - CONNECT : l'en-tête « Authorization: Bearer … » doit porter un jeton valide (signature, expiration, émetteur)
 *   d'un compte actif ; la connexion prend alors le nom de ce compte (son identifiant).
 * - SUBSCRIBE : seulement sur sa propre file « /user/queue/… » ; le courtier la relie au compte connecté,
 *   donc personne ne peut écouter les notifications d'un autre.
 * - SEND : refusé, le navigateur ne fait qu'écouter.
 */
@Component
@RequiredArgsConstructor
public class ControleConnexionTempsReel implements ChannelInterceptor {

    private static final String PREFIXE_JETON = "Bearer ";

    private final JwtDecoder decodeurJeton;
    private final LectureCompteConnecte lectureCompteConnecte;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel canal) {
        StompHeaderAccessor entetes = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (entetes == null || entetes.getCommand() == null) {
            return message;
        }
        switch (entetes.getCommand()) {
            // Certains clients envoient STOMP au lieu de CONNECT : même contrôle
            case CONNECT, STOMP -> entetes.setUser(authentifier(entetes.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> verifierAbonnement(entetes);
            case SEND -> throw new MessageDeliveryException("L'envoi de messages n'est pas autorisé.");
            default -> { }
        }
        return message;
    }

    private AbstractAuthenticationToken authentifier(String entete) {
        if (entete == null || !entete.startsWith(PREFIXE_JETON)) {
            throw new MessageDeliveryException("Jeton d'accès manquant.");
        }
        try {
            return lectureCompteConnecte.convert(decodeurJeton.decode(entete.substring(PREFIXE_JETON.length())));
        } catch (JwtException | AuthenticationException refus) {
            throw new MessageDeliveryException("Jeton d'accès refusé.");
        }
    }

    private static void verifierAbonnement(StompHeaderAccessor entetes) {
        String destination = entetes.getDestination();
        if (entetes.getUser() == null || destination == null || !destination.startsWith("/user/queue/")) {
            throw new MessageDeliveryException("Abonnement refusé.");
        }
    }
}
