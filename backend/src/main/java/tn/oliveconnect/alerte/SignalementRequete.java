package tn.oliveconnect.alerte;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Formulaire « Signaler maturité ou problème » : verger, type, position et description. */
public record SignalementRequete(
        @NotBlank(message = "Choisissez le verger.")
        String vergerId,

        @NotNull(message = "Choisissez le type de signalement.")
        TypeAlerte type,

        @NotNull(message = "Indiquez la position (GPS ou carte).")
        @DecimalMin(value = "34.0", message = "Position hors de la région de Sfax.")
        @DecimalMax(value = "35.5", message = "Position hors de la région de Sfax.")
        Double latitude,

        @NotNull(message = "Indiquez la position (GPS ou carte).")
        @DecimalMin(value = "9.5", message = "Position hors de la région de Sfax.")
        @DecimalMax(value = "11.5", message = "Position hors de la région de Sfax.")
        Double longitude,

        @NotBlank(message = "Décrivez ce que vous observez.")
        @Size(max = 500, message = "500 caractères au maximum.")
        String description) {
}
