package tn.oliveconnect.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motif du refus d'une inscription, communiqué à l'agriculteur. */
public record RefusInscriptionRequete(
        @NotBlank(message = "Indiquez le motif du refus.")
        @Size(max = 300, message = "300 caractères au maximum.")
        String motif) {
}
