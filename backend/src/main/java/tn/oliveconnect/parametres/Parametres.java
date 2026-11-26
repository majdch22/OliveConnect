package tn.oliveconnect.parametres;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Réglages de la plateforme modifiables par l'administrateur, sans redémarrer le serveur.
 * Un seul document dans la collection « parametres », rangé en sections (sous-documents imbriqués).
 *
 * @param id          toujours {@value #IDENTIFIANT}
 * @param modifieLe   date de la dernière modification
 * @param modifiePar  nom de l'administrateur qui l'a faite
 */
@Document(collection = "parametres")
public record Parametres(
        @Id String id,
        Campagne campagne,
        ReglesMetier regles,
        Imports imports,
        Notifications notifications,
        Securite securite,
        Instant modifieLe,
        String modifiePar) {

    public static final String IDENTIFIANT = "PARAMETRES";

    /** Période de la campagne de récolte (en général de novembre à février). */
    public record Campagne(LocalDate debut, LocalDate fin) {
    }

    /**
     * @param seuilAlerteMaturite        maturité (%) à partir de laquelle une alerte de maturité est créée automatiquement
     * @param tolerancePesee             poids maximal accepté au pressoir, en % de la capacité de la benne
     * @param distanceMaximaleSignalement distance (m) au-delà de laquelle un signalement est jugé loin de son verger
     * @param delaiCibleUrgences         délai visé (h) entre une demande urgente et sa collecte
     */
    public record ReglesMetier(int seuilAlerteMaturite, int tolerancePesee, int distanceMaximaleSignalement,
            int delaiCibleUrgences) {
    }

    /** Fréquence de chaque import automatique. */
    public record Imports(FrequenceImport referentiels, FrequenceImport meteoEtMaturite, FrequenceImport capacitePressoirs) {
    }

    /** Canaux de notification activés. */
    public record Notifications(boolean smsAgriculteurs, boolean emailRecapitulatifQuotidien) {
    }

    /**
     * @param dureeSessionHeures      durée de validité d'une connexion
     * @param tentativesAvantBlocage  mots de passe incorrects tolérés avant le blocage temporaire
     * @param dureeBlocageMinutes     durée du blocage temporaire
     */
    public record Securite(int dureeSessionHeures, int tentativesAvantBlocage, int dureeBlocageMinutes) {
    }
}
