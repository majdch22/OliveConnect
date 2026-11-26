package tn.oliveconnect.configuration;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tn.oliveconnect.security.JetonService;
import tn.oliveconnect.security.LectureCompteConnecte;
import tn.oliveconnect.security.ReponsesSecurite;

/**
 * Règles de sécurité du serveur.
 * - Sans état : aucune session serveur, chaque requête porte son jeton d'accès (en-tête Authorization: Bearer).
 *   Pas de cookie de session, donc pas de protection CSRF à activer.
 * - Seules la connexion, l'inscription, le mot de passe oublié, la carte publique et l'état de santé sont publics ;
 *   tout le reste exige un jeton valide (pour le temps réel, le jeton est vérifié dans la connexion STOMP).
 * - Jeton vérifié : signature HS256, date d'expiration et émetteur « oliveconnect ».
 * - À chaque requête, le compte est relu en base ({@link LectureCompteConnecte}) : un compte désactivé perd l'accès
 *   immédiatement, et son rôle devient une autorité Spring « ROLE_... », utilisable avec @PreAuthorize.
 * - Les refus (401, 403) sont renvoyés au format JSON habituel ({@link ReponsesSecurite}).
 * - En-têtes de protection : pas d'affichage dans un cadre, pas de devinette du type de contenu, pas de « referrer ».
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain reglesDeSecurite(HttpSecurity http, LectureCompteConnecte lectureCompteConnecte,
            ReponsesSecurite reponsesSecurite) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acces -> acces
                        .requestMatchers(HttpMethod.POST, "/api/connexion", "/api/administration/connexion").permitAll()
                        .requestMatchers("/api/inscription", "/api/inscription/delegations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mot-de-passe-oublie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Ouverture du WebSocket : le jeton est vérifié ensuite, dans le message STOMP de connexion
                        .requestMatchers("/ws").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(serveur -> serveur
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(lectureCompteConnecte))
                        .authenticationEntryPoint(reponsesSecurite)
                        .accessDeniedHandler(reponsesSecurite))
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint(reponsesSecurite)
                        .accessDeniedHandler(reponsesSecurite))
                .headers(entetes -> entetes
                        .frameOptions(cadres -> cadres.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(politique -> politique.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
        return http.build();
    }

    /** BCrypt : chiffrement à sens unique, on compare sans jamais pouvoir retrouver le mot de passe. */
    @Bean
    public PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder encodeurJeton(OliveConnectProperties proprietes) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cleSecrete(proprietes)));
    }

    /** Vérifie la signature, la date d'expiration et l'émetteur de chaque jeton reçu. */
    @Bean
    public JwtDecoder decodeurJeton(OliveConnectProperties proprietes) {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder.withSecretKey(cleSecrete(proprietes)).macAlgorithm(MacAlgorithm.HS256).build();
        decodeur.setJwtValidator(JwtValidators.createDefaultWithIssuer(JetonService.EMETTEUR));
        return decodeur;
    }

    /** Clé de signature des jetons (au moins 32 caractères, fournie par la configuration). */
    private SecretKey cleSecrete(OliveConnectProperties proprietes) {
        byte[] octets = proprietes.securite().secretJeton().getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException("Le secret des jetons doit contenir au moins 32 caractères.");
        }
        return new SecretKeySpec(octets, "HmacSHA256");
    }
}
