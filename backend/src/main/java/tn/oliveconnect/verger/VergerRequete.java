package tn.oliveconnect.verger;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.oliveconnect.utilisateur.Delegation;

/** Formulaire d'ajout ou de modification d'un verger par son agriculteur. Le code est attribué par la plateforme. */
public record VergerRequete(
        @NotBlank(message = "Saisissez le nom du verger.")
        @Size(max = 60, message = "60 caractères au maximum.")
        String nom,

        @NotNull(message = "Placez le verger sur la carte.")
        @DecimalMin(value = "34.0", message = "Position hors de la région de Sfax.")
        @DecimalMax(value = "35.5", message = "Position hors de la région de Sfax.")
        Double latitude,

        @NotNull(message = "Placez le verger sur la carte.")
        @DecimalMin(value = "9.5", message = "Position hors de la région de Sfax.")
        @DecimalMax(value = "11.5", message = "Position hors de la région de Sfax.")
        Double longitude,

        @NotNull(message = "Choisissez la délégation.")
        Delegation delegation,

        @NotNull(message = "Saisissez la superficie.")
        @DecimalMin(value = "0.1", message = "Entre 0,1 et 500 hectares.")
        @DecimalMax(value = "500", message = "Entre 0,1 et 500 hectares.")
        Double superficieHa,

        @Min(value = 1, message = "Entre 1 et 100 000 arbres.")
        @Max(value = 100000, message = "Entre 1 et 100 000 arbres.")
        int nombreArbres,

        @NotBlank(message = "Choisissez la variété.")
        String varieteId) {
}
