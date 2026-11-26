package tn.oliveconnect.parametres;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Formulaire des paramètres. Chaque valeur est bornée pour qu'une faute de frappe
 * ne puisse pas rendre la plateforme inutilisable (par exemple une session de 0 heure).
 * Les noms des champs d'erreur suivent la structure : « regles.seuilAlerteMaturite », « securite.dureeSessionHeures »…
 */
public record ParametresRequete(
        @NotNull @Valid Campagne campagne,
        @NotNull @Valid Regles regles,
        @NotNull @Valid Imports imports,
        @NotNull @Valid Notifications notifications,
        @NotNull @Valid Securite securite) {

    public record Campagne(
            @NotNull(message = "Indiquez la date de début.") LocalDate debut,
            @NotNull(message = "Indiquez la date de fin.") LocalDate fin) {
    }

    public record Regles(
            @Min(value = 50, message = "Entre 50 et 100 %.") @Max(value = 100, message = "Entre 50 et 100 %.")
            int seuilAlerteMaturite,
            @Min(value = 100, message = "Entre 100 et 150 %.") @Max(value = 150, message = "Entre 100 et 150 %.")
            int tolerancePesee,
            @Min(value = 50, message = "Entre 50 et 5 000 m.") @Max(value = 5000, message = "Entre 50 et 5 000 m.")
            int distanceMaximaleSignalement,
            @Min(value = 1, message = "Entre 1 et 168 h.") @Max(value = 168, message = "Entre 1 et 168 h.")
            int delaiCibleUrgences) {
    }

    public record Imports(
            @NotNull(message = "Choisissez une fréquence.") FrequenceImport referentiels,
            @NotNull(message = "Choisissez une fréquence.") FrequenceImport meteoEtMaturite,
            @NotNull(message = "Choisissez une fréquence.") FrequenceImport capacitePressoirs) {
    }

    public record Notifications(boolean smsAgriculteurs, boolean emailRecapitulatifQuotidien) {
    }

    public record Securite(
            @Min(value = 1, message = "Entre 1 et 24 h.") @Max(value = 24, message = "Entre 1 et 24 h.")
            int dureeSessionHeures,
            @Min(value = 3, message = "Entre 3 et 10 tentatives.") @Max(value = 10, message = "Entre 3 et 10 tentatives.")
            int tentativesAvantBlocage,
            @Min(value = 5, message = "Entre 5 et 60 minutes.") @Max(value = 60, message = "Entre 5 et 60 minutes.")
            int dureeBlocageMinutes) {
    }
}
