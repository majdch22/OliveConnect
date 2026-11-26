package tn.oliveconnect.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Réponses de la couche de sécurité, au même format JSON que les autres erreurs ({@code ReponseErreur}) :
 * - 401 : pas de jeton, jeton expiré, falsifié, ou compte désactivé depuis la connexion ;
 * - 403 : jeton valide mais rôle insuffisant.
 * Les messages sont fixes : rien de ce que le client envoie n'est recopié dans la réponse.
 */
@Component
public class ReponsesSecurite implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest requete, HttpServletResponse reponse, AuthenticationException exception)
            throws IOException {
        reponse.setHeader("WWW-Authenticate", "Bearer");
        ecrire(reponse, HttpStatus.UNAUTHORIZED, "NON_CONNECTE",
                "Votre session a expiré ou n'est plus valide. Reconnectez-vous.");
    }

    @Override
    public void handle(HttpServletRequest requete, HttpServletResponse reponse, AccessDeniedException exception)
            throws IOException {
        ecrire(reponse, HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Votre profil ne permet pas cette action.");
    }

    private static void ecrire(HttpServletResponse reponse, HttpStatus statut, String code, String message) throws IOException {
        reponse.setStatus(statut.value());
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write("""
                {"statut":%d,"code":"%s","message":"%s","champs":{},"horodatage":"%s"}"""
                .formatted(statut.value(), code, message, Instant.now()));
    }
}
