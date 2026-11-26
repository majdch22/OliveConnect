package tn.oliveconnect.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.security.ControleConnexionTempsReel;

/**
 * Notifications en temps réel (décision 13) : WebSocket avec le protocole STOMP, à l'adresse /ws.
 * - Le navigateur s'abonne à sa file personnelle « /user/queue/notifications » ; le serveur y pousse ses notifications.
 * - Le jeton d'accès est vérifié à la connexion STOMP ({@link ControleConnexionTempsReel}) : l'ouverture du WebSocket
 *   elle-même est publique, car un navigateur ne peut pas y joindre d'en-tête Authorization.
 * - Seules les pages de l'application (origines autorisées) peuvent ouvrir la connexion.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class TempsReelConfiguration implements WebSocketMessageBrokerConfigurer {

    private final OliveConnectProperties proprietes;
    private final ControleConnexionTempsReel controleConnexion;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registre) {
        registre.addEndpoint("/ws").setAllowedOriginPatterns(proprietes.tempsReel().originesAutorisees().split("\\s*,\\s*"));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registre) {
        // Courtier simple en mémoire : suffisant pour un seul serveur
        registre.enableSimpleBroker("/queue");
        registre.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration canal) {
        canal.interceptors(controleConnexion);
    }
}
