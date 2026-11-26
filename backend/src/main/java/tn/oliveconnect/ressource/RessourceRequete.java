package tn.oliveconnect.ressource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire d'ajout ou de modification d'une ressource. Le code est attribué par la plateforme.
 * Les champs propres à un type (capacité d'une benne, quantité d'un matériel, équipe d'un ouvrier)
 * sont vérifiés dans {@link RessourceService}.
 */
public record RessourceRequete(
        @NotNull(message = "Choisissez le type de ressource.")
        TypeRessource type,

        @NotBlank(message = "Saisissez le nom.")
        @Size(max = 80, message = "80 caractères au maximum.")
        String nom,

        /** Facultatif : immatriculation d'un tracteur ou d'une benne. */
        String matricule,

        /** Obligatoire pour une benne. */
        Integer capaciteKg,

        /** Obligatoire pour un matériel. */
        Integer quantite,

        /** Facultatif : équipe d'un ouvrier. */
        String equipeId,

        /** Facultatif : téléphone d'un ouvrier. */
        String telephone,

        @NotNull(message = "Choisissez l'état.")
        EtatRessource etat) {
}
