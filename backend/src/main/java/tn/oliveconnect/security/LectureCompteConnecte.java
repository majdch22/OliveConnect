package tn.oliveconnect.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.supervision.SuiviActivite;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * À chaque requête, relit le compte indiqué par le jeton d'accès.
 * - Si le compte a été désactivé (ou supprimé) depuis la connexion, l'accès est coupé tout de suite (réponse 401).
 * - Le rôle vient de la base et non du jeton : un changement de rôle par l'administrateur s'applique immédiatement.
 * - La requête est notée pour compter les utilisateurs actifs (page de supervision).
 */
@Component
@RequiredArgsConstructor
public class LectureCompteConnecte implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UtilisateurRepository utilisateurRepository;
    private final SuiviActivite suiviActivite;

    @Override
    public AbstractAuthenticationToken convert(Jwt jeton) {
        Utilisateur compte = utilisateurRepository.findById(jeton.getSubject())
                .filter(Utilisateur::estActif)
                .orElseThrow(() -> new DisabledException("Ce compte n'est plus actif."));
        suiviActivite.noterRequete(compte.getId());
        var autorite = new SimpleGrantedAuthority("ROLE_" + compte.getRole().name());
        return new JwtAuthenticationToken(jeton, List.of(autorite), compte.getId());
    }
}
