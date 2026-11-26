package tn.oliveconnect.utilisateur;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compte d'une personne qui utilise la plateforme (agriculteur ou membre du personnel).
 * Collection MongoDB : « utilisateurs ».
 * L'index composé (rôle, statut) sert les filtres de la liste des utilisateurs de l'administration.
 */
@Document(collection = "utilisateurs")
@CompoundIndex(name = "role_statut", def = "{'role': 1, 'statut': 1}")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    private String id;

    private String nom;
    private String prenom;

    /** Facultatif : l'index unique « sparse » ignore les comptes sans e-mail. */
    @Indexed(unique = true, sparse = true)
    private String email;

    /** Toujours au format +216XXXXXXXX (voir {@link Identifiants}). */
    @Indexed(unique = true)
    private String telephone;

    /** Mot de passe chiffré avec BCrypt, jamais en clair. */
    private String motDePasse;

    /** Vrai si le mot de passe a été donné par l'administrateur (création ou réinitialisation) et doit être changé. */
    private boolean motDePasseProvisoire;

    private Role role;
    private StatutCompte statut;

    /** Renseigné uniquement pour les agriculteurs. */
    private InfosAgriculteur agriculteur;

    /** Pressoir d'un responsable du pressoir : il n'agit que sur ce pressoir. Vide pour les autres rôles. */
    @Indexed(sparse = true)
    private String pressoirId;

    /** Échecs de mot de passe consécutifs depuis la dernière connexion réussie. */
    private int tentativesEchouees;

    /** Si renseigné et dans le futur, le compte est bloqué jusqu'à cette date. */
    private Instant bloqueJusqua;

    private Instant derniereConnexion;

    /** Vide tant que la personne n'a rien choisi : voir {@link #preferences()}. */
    private PreferencesNotification preferencesNotification;

    /** Vrai une fois la visite guidée terminée ou passée : elle n'est plus proposée à la connexion. */
    private boolean visiteGuideeTerminee;

    @CreatedDate
    private Instant dateCreation;

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    /** Préférences de notification, avec les valeurs par défaut si la personne n'a rien choisi. */
    public PreferencesNotification preferences() {
        return preferencesNotification == null ? PreferencesNotification.PAR_DEFAUT : preferencesNotification;
    }

    public boolean estActif() {
        return statut == StatutCompte.ACTIF;
    }

    public boolean estBloque(Instant maintenant) {
        return bloqueJusqua != null && bloqueJusqua.isAfter(maintenant);
    }

    /**
     * Compte un mot de passe incorrect.
     * Au bout du nombre maximal d'échecs, le compte est bloqué pour la durée indiquée et le compteur repart à zéro.
     */
    public void enregistrerEchecConnexion(int tentativesMaximum, Duration dureeBlocage, Instant maintenant) {
        tentativesEchouees++;
        if (tentativesEchouees >= tentativesMaximum) {
            bloqueJusqua = maintenant.plus(dureeBlocage);
            tentativesEchouees = 0;
        }
    }

    /** Remet les compteurs à zéro après une connexion réussie. */
    public void enregistrerConnexionReussie(Instant maintenant) {
        tentativesEchouees = 0;
        bloqueJusqua = null;
        derniereConnexion = maintenant;
    }
}
