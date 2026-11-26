package tn.oliveconnect.supervision;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Retient le moment de la dernière requête de chaque compte, pour compter les utilisateurs actifs.
 * Les connexions sont sans session côté serveur : « actif » signifie « a fait une requête dans les 15 dernières minutes ».
 */
@Component
@RequiredArgsConstructor
public class SuiviActivite {

    public static final Duration FENETRE_ACTIVITE = Duration.ofMinutes(15);

    private final Clock horloge;
    private final Map<String, Instant> derniereRequeteParCompte = new ConcurrentHashMap<>();

    public void noterRequete(String compteId) {
        derniereRequeteParCompte.put(compteId, horloge.instant());
    }

    /** Nombre de comptes actifs récemment ; les comptes inactifs sont oubliés au passage. */
    public int nombreUtilisateursActifs() {
        Instant limite = horloge.instant().minus(FENETRE_ACTIVITE);
        derniereRequeteParCompte.values().removeIf(derniereRequete -> derniereRequete.isBefore(limite));
        return derniereRequeteParCompte.size();
    }
}
