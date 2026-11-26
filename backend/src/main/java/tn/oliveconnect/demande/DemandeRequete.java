package tn.oliveconnect.demande;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire « Déclarer une récolte » : quatre saisies (verger, type, date, quantité) et un commentaire facultatif.
 * Les règles de date (aujourd'hui pour une urgence, demain au plus tôt sinon, dans la campagne) sont dans {@link DemandeRecolteService}.
 */
public record DemandeRequete(
        @NotBlank(message = "Choisissez le verger.")
        String vergerId,

        @NotNull(message = "Choisissez le type de récolte.")
        TypeRecolte type,

        @NotNull(message = "Choisissez la date souhaitée.")
        LocalDate dateSouhaitee,

        @Min(value = 1, message = "Entre 1 et 100 000 kg.")
        @Max(value = 100000, message = "Entre 1 et 100 000 kg.")
        int quantiteEstimeeKg,

        @Size(max = 300, message = "300 caractères au maximum.")
        String commentaire) {
}
