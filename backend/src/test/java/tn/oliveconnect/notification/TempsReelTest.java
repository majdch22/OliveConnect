package tn.oliveconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import tn.oliveconnect.security.JetonService;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;

/**
 * Notifications en temps réel avec un vrai serveur et un vrai client WebSocket STOMP (décision 13) :
 * sans jeton la connexion est refusée ; avec un jeton, la personne reçoit ses notifications à l'instant où elles partent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TempsReelTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JetonService jetonService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder encodeur;

    private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    private Utilisateur karim;

    @BeforeEach
    void preparer() {
        utilisateurRepository.findByTelephone("+21650009999").ifPresent(utilisateurRepository::delete);
        karim = new Utilisateur();
        karim.setPrenom("Karim");
        karim.setNom("Trabelsi");
        karim.setTelephone("+21650009999");
        karim.setMotDePasse(encodeur.encode("Olive2026!"));
        karim.setRole(Role.RESPONSABLE_COLLECTE);
        karim.setStatut(StatutCompte.ACTIF);
        karim = utilisateurRepository.save(karim);
    }

    @AfterEach
    void nettoyer() {
        client.stop();
        utilisateurRepository.delete(karim);
    }

    @Test
    void sans_jeton_la_connexion_temps_reel_est_refusee() {
        assertThatThrownBy(() -> connecter(new StompHeaders()).get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void avec_un_jeton_valide_la_notification_arrive_en_temps_reel() throws Exception {
        StompHeaders entetes = new StompHeaders();
        entetes.add("Authorization", "Bearer " + jetonService.generer(karim).valeur());
        StompSession session = connecter(entetes).get(5, TimeUnit.SECONDS);
        BlockingQueue<String> recues = new ArrayBlockingQueue<>(1);
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders entetesRecues) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders entetesRecues, Object contenu) {
                recues.add(new String((byte[]) contenu, StandardCharsets.UTF_8));
            }
        });
        // Laisse le temps à l'abonnement d'être enregistré par le serveur
        Thread.sleep(300);

        notificationService.notifierPersonne(karim.getId(),
                NouvelleNotification.simple(CategorieNotification.ALERTE, "Ravageur · Verger Nord", "Mouche de l'olive", null));

        assertThat(recues.poll(5, TimeUnit.SECONDS)).contains("Ravageur · Verger Nord");
    }

    private CompletableFuture<StompSession> connecter(StompHeaders entetes) {
        WebSocketHttpHeaders entetesOuverture = new WebSocketHttpHeaders();
        entetesOuverture.setOrigin("http://localhost:4200");
        return client.connectAsync("ws://localhost:" + port + "/ws", entetesOuverture, entetes, new StompSessionHandlerAdapter() {
        });
    }
}
