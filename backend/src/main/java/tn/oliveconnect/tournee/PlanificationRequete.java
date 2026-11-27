package tn.oliveconnect.tournee;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire « Nouvelle tournée » du responsable de collecte : les demandes validées choisies (dans l'ordre de passage),
 * la date, le chef d'équipe, le pressoir de destination et les ressources affectées.
 * Les règles (ressource prise une seule fois par jour, capacité des bennes, capacité du pressoir…) sont dans
 * {@link PlanificationService}.
 */
public record PlanificationRequete(
        @NotNull(message = "Choisissez la date de la tournée.")
        LocalDate date,

        @NotEmpty(message = "Choisissez au moins une demande validée.")
        @Size(max = 20, message = "20 vergers au plus par tournée.")
        List<@NotBlank String> demandeIds,

        @NotBlank(message = "Choisissez le chef d'équipe.")
        String chefEquipeId,

        @NotBlank(message = "Choisissez le pressoir de destination.")
        String pressoirId,

        @Size(max = 60, message = "60 ressources au plus par tournée.")
        List<@NotBlank String> ressourceIds,

        @Size(max = 300, message = "300 caractères au maximum.")
        String commentaire) {

    public List<String> ressources() {
        return ressourceIds == null ? List.of() : ressourceIds.stream().distinct().toList();
    }
}
