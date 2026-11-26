package tn.oliveconnect.supervision;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;

/**
 * Garde en mémoire les derniers avertissements et erreurs écrits dans le journal du serveur,
 * pour les afficher sur la page de supervision sans avoir à ouvrir les fichiers de journal.
 */
@Component
public class JournalRecent extends AppenderBase<ILoggingEvent> {

    private static final int NOMBRE_MAXIMUM = 50;

    /** Un message du journal : moment, niveau (AVERTISSEMENT ou ERREUR), origine et texte. */
    public record Evenement(Instant moment, String niveau, String origine, String message) {
    }

    private final Deque<Evenement> evenements = new ArrayDeque<>();

    /** Se branche sur le journal principal au démarrage. */
    @PostConstruct
    void brancher() {
        LoggerContext contexte = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(contexte);
        setName("journal-recent");
        start();
        contexte.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
    }

    @Override
    protected synchronized void append(ILoggingEvent evenement) {
        if (!evenement.getLevel().isGreaterOrEqual(Level.WARN)) {
            return;
        }
        String niveau = evenement.getLevel() == Level.ERROR ? "ERREUR" : "AVERTISSEMENT";
        String origine = evenement.getLoggerName().substring(evenement.getLoggerName().lastIndexOf('.') + 1);
        evenements.addFirst(new Evenement(Instant.ofEpochMilli(evenement.getTimeStamp()), niveau, origine,
                evenement.getFormattedMessage()));
        while (evenements.size() > NOMBRE_MAXIMUM) {
            evenements.removeLast();
        }
    }

    /** Les événements, du plus récent au plus ancien. */
    public synchronized List<Evenement> derniers() {
        return new ArrayList<>(evenements);
    }
}
