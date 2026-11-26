package tn.oliveconnect.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.parametres.ParametresService;
import tn.oliveconnect.utilisateur.Utilisateur;

/**
 * Fabrique le jeton d'accès remis après une connexion réussie.
 * Le jeton contient l'identifiant et le rôle du compte ; il est signé, donc impossible à modifier côté navigateur.
 * Sa durée de validité est la durée de session des paramètres.
 */
@Service
@RequiredArgsConstructor
public class JetonService {

    /** Nom de l'information du jeton qui contient le rôle (lue par la configuration de sécurité). */
    public static final String INFORMATION_ROLE = "role";

    /** Émetteur inscrit dans chaque jeton et vérifié à chaque requête. */
    public static final String EMETTEUR = "oliveconnect";

    private final JwtEncoder encodeurJeton;
    private final ParametresService parametresService;
    private final Clock horloge;

    public JetonGenere generer(Utilisateur utilisateur) {
        Instant maintenant = horloge.instant();
        Instant expiration = maintenant.plus(parametresService.dureeSession());

        JwtClaimsSet informations = JwtClaimsSet.builder()
                .issuer(EMETTEUR)
                .subject(utilisateur.getId())
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .claim(INFORMATION_ROLE, utilisateur.getRole().name())
                .build();

        JwsHeader entete = JwsHeader.with(MacAlgorithm.HS256).build();
        String valeur = encodeurJeton.encode(JwtEncoderParameters.from(entete, informations)).getTokenValue();
        return new JetonGenere(valeur, expiration);
    }

    /** Jeton signé et sa date de fin de validité. */
    public record JetonGenere(String valeur, Instant expiration) {
    }
}
