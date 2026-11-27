package tn.oliveconnect.pressoir;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire « Réceptionner une benne » (maquette M7) : la tournée, la benne, le poids net et l'acidité.
 * La catégorie de qualité et le taux de remplissage sont calculés par le serveur.
 */
public record ReceptionRequete(
        @NotBlank(message = "Choisissez la tournée.")
        String tourneeId,

        @NotBlank(message = "Choisissez la benne.")
        String benneId,

        @Min(value = 1, message = "Entre 1 et 50 000 kg.")
        @Max(value = 50000, message = "Entre 1 et 50 000 kg.")
        int poidsNetKg,

        @NotNull(message = "Indiquez l'acidité mesurée.")
        @DecimalMin(value = "0.0", message = "Entre 0 et 20 %.")
        @DecimalMax(value = "20.0", message = "Entre 0 et 20 %.")
        Double aciditePourcent,

        @Size(max = 300, message = "300 caractères au maximum.")
        String commentaire) {
}
