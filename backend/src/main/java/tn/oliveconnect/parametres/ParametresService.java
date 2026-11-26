package tn.oliveconnect.parametres;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.configuration.OliveConnectProperties;

/**
 * Lecture et modification des paramètres.
 * Les valeurs sont gardées en mémoire (lues à chaque connexion, à chaque jeton…) et rechargées après chaque modification :
 * un changement s'applique immédiatement, sans redémarrage.
 * Au tout premier démarrage, le document est créé avec les valeurs par défaut.
 */
@Service
@RequiredArgsConstructor
public class ParametresService {

    private final ParametresRepository parametresRepository;
    private final OliveConnectProperties proprietes;
    private final Clock horloge;

    private final AtomicReference<Parametres> enMemoire = new AtomicReference<>();

    public Parametres actuels() {
        Parametres parametres = enMemoire.get();
        if (parametres == null) {
            parametres = parametresRepository.findById(Parametres.IDENTIFIANT)
                    .orElseGet(() -> parametresRepository.save(valeursParDefaut()));
            enMemoire.set(parametres);
        }
        return parametres;
    }

    public Parametres modifier(ParametresRequete requete, String auteur) {
        if (!requete.campagne().debut().isBefore(requete.campagne().fin())) {
            throw new ErreurMetierException(HttpStatus.BAD_REQUEST, "DONNEES_INVALIDES",
                    "Certains champs sont manquants ou incorrects.",
                    Map.of("campagne.fin", "La fin de la campagne doit être après son début."));
        }
        Parametres nouveaux = new Parametres(
                Parametres.IDENTIFIANT,
                new Parametres.Campagne(requete.campagne().debut(), requete.campagne().fin()),
                new Parametres.ReglesMetier(requete.regles().seuilAlerteMaturite(), requete.regles().tolerancePesee(),
                        requete.regles().distanceMaximaleSignalement(), requete.regles().delaiCibleUrgences()),
                new Parametres.Imports(requete.imports().referentiels(), requete.imports().meteoEtMaturite(),
                        requete.imports().capacitePressoirs()),
                new Parametres.Notifications(requete.notifications().smsAgriculteurs(),
                        requete.notifications().emailRecapitulatifQuotidien()),
                new Parametres.Securite(requete.securite().dureeSessionHeures(),
                        requete.securite().tentativesAvantBlocage(), requete.securite().dureeBlocageMinutes()),
                horloge.instant(),
                auteur);
        enMemoire.set(parametresRepository.save(nouveaux));
        return nouveaux;
    }

    /** Oublie la copie en mémoire : la prochaine lecture repart de la base (utilisé par les tests). */
    public void relireDepuisLaBase() {
        enMemoire.set(null);
    }

    // --- Raccourcis utilisés par les autres services ---

    public Duration dureeSession() {
        return Duration.ofHours(actuels().securite().dureeSessionHeures());
    }

    public int tentativesAvantBlocage() {
        return actuels().securite().tentativesAvantBlocage();
    }

    public Duration dureeBlocage() {
        return Duration.ofMinutes(actuels().securite().dureeBlocageMinutes());
    }

    /** Valeurs du premier démarrage : sécurité reprise de application.yml, campagne de l'année en cours. */
    private Parametres valeursParDefaut() {
        OliveConnectProperties.Securite securite = proprietes.securite();
        int annee = LocalDate.now(horloge).getMonthValue() >= 9
                ? LocalDate.now(horloge).getYear()
                : LocalDate.now(horloge).getYear() - 1;
        return new Parametres(
                Parametres.IDENTIFIANT,
                new Parametres.Campagne(LocalDate.of(annee, 11, 15), LocalDate.of(annee + 1, 2, 28)),
                new Parametres.ReglesMetier(70, 110, 500, 24),
                new Parametres.Imports(FrequenceImport.CHAQUE_JOUR, FrequenceImport.TOUTES_LES_15_MINUTES,
                        FrequenceImport.TOUTES_LES_15_MINUTES),
                new Parametres.Notifications(true, false),
                new Parametres.Securite((int) securite.dureeJeton().toHours(), securite.tentativesAvantBlocage(),
                        (int) securite.dureeBlocage().toMinutes()),
                horloge.instant(),
                "la plateforme (valeurs par défaut)");
    }
}
