package tn.oliveconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Vérifie que l'application démarre avec toute sa configuration. */
@SpringBootTest
@ActiveProfiles("test")
class OliveConnectApplicationTests {

    @Test
    void application_demarre() {
    }
}
