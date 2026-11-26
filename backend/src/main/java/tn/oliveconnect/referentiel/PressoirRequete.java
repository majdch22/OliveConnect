package tn.oliveconnect.referentiel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.oliveconnect.utilisateur.Delegation;

/**
 * Formulaire de création ou de modification d'un pressoir. Le code est attribué par la plateforme.
 * La position est limitée à la région de Sfax (et ses îles), zone de la coopérative.
 */
public record PressoirRequete(
        @NotBlank(message = "Saisissez le nom du pressoir.")
        @Size(max = 60, message = "60 caractères au maximum.")
        String nom,

        @NotNull(message = "Choisissez la délégation.")
        Delegation delegation,

        @NotNull(message = "Saisissez la latitude.")
        @DecimalMin(value = "34.0", message = "Latitude hors de la région de Sfax (entre 34 et 35,5).")
        @DecimalMax(value = "35.5", message = "Latitude hors de la région de Sfax (entre 34 et 35,5).")
        Double latitude,

        @NotNull(message = "Saisissez la longitude.")
        @DecimalMin(value = "9.5", message = "Longitude hors de la région de Sfax (entre 9,5 et 11,5).")
        @DecimalMax(value = "11.5", message = "Longitude hors de la région de Sfax (entre 9,5 et 11,5).")
        Double longitude,

        @Min(value = 1000, message = "Entre 1 000 et 500 000 kg par jour.")
        @Max(value = 500000, message = "Entre 1 000 et 500 000 kg par jour.")
        int capaciteJournaliereKg,

        /** Facultatif. */
        String telephone) {
}
