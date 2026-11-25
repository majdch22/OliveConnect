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
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tn.oliveconnect.security.JetonService;

/**
 * Règles de sécurité du serveur.
 * - Sans état : aucune session serveur, chaque requête porte son jeton d'accès (en-tête Authorization: Bearer).
 * - Seules la connexion, l'inscription et l'état de santé sont publiques ; tout le reste exige un jeton valide.
 * - Le rôle lu dans le jeton devient une autorité Spring « ROLE_... », utilisable avec @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain reglesDeSecurite(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acces -> acces
                        .requestMatchers(HttpMethod.POST, "/api/connexion", "/api/administration/connexion").permitAll()
                        .requestMatchers("/api/inscription", "/api/inscription/delegations").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(serveur -> serveur
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(lectureDuRole())));
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

    @Bean
    public JwtDecoder decodeurJeton(OliveConnectProperties proprietes) {
        return NimbusJwtDecoder.withSecretKey(cleSecrete(proprietes)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Transforme l'information « role » du jeton en autorité Spring (ex. ROLE_AGRICULTEUR). */
    private JwtAuthenticationConverter lectureDuRole() {
        JwtGrantedAuthoritiesConverter autorites = new JwtGrantedAuthoritiesConverter();
        autorites.setAuthoritiesClaimName(JetonService.INFORMATION_ROLE);
        autorites.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertisseur = new JwtAuthenticationConverter();
        convertisseur.setJwtGrantedAuthoritiesConverter(autorites);
        return convertisseur;
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
