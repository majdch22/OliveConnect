package tn.oliveconnect.referentiel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulaire de création ou de modification d'une variété. Le code est attribué par la plateforme. */
public record VarieteRequete(
        @NotBlank(message = "Saisissez le nom de la variété.")
        @Size(max = 40, message = "40 caractères au maximum.")
        String nom,

        @Min(value = 500, message = "Entre 500 et 20 000 kg par hectare.")
        @Max(value = 20000, message = "Entre 500 et 20 000 kg par hectare.")
        int rendementMoyenKgParHectare,

        @Min(value = 1, message = "Choisissez un mois.") @Max(value = 12, message = "Choisissez un mois.")
        int moisDebutRecolte,

        @Min(value = 1, message = "Choisissez un mois.") @Max(value = 12, message = "Choisissez un mois.")
        int moisFinRecolte) {
}
