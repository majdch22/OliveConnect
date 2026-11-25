package tn.oliveconnect.inscription;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tn.oliveconnect.utilisateur.Delegation;

/**
 * Formulaire d'inscription d'un agriculteur.
 * Les contrôles simples sont faits ici (champ par champ) ; ceux qui demandent la base
 * (téléphone ou CIN déjà utilisés) sont faits dans {@link InscriptionService}.
 */
public record InscriptionRequete(
        @NotBlank(message = "Saisissez votre nom.")
        @Size(max = 60, message = "60 caractères au maximum.")
        String nom,

        @NotBlank(message = "Saisissez votre prénom.")
        @Size(max = 60, message = "60 caractères au maximum.")
        String prenom,

        @NotBlank(message = "Saisissez votre numéro de carte d'identité.")
        @Pattern(regexp = "\\d{8}", message = "La carte d'identité comporte 8 chiffres.")
        String cin,

        @NotBlank(message = "Saisissez votre numéro de téléphone.")
        String telephone,

        /** Facultatif. */
        @Email(message = "Cette adresse e-mail n'est pas valide.")
        String email,

        @NotNull(message = "Choisissez votre délégation.")
        Delegation delegation,

        @NotBlank(message = "Choisissez un mot de passe.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "8 caractères minimum, dont au moins une lettre et un chiffre.")
        String motDePasse,

        @NotBlank(message = "Confirmez votre mot de passe.")
        String confirmationMotDePasse,

        @AssertTrue(message = "Vous devez accepter les conditions d'utilisation.")
        boolean conditionsAcceptees) {
}
