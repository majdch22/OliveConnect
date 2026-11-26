package tn.oliveconnect.ressource;

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
 * Ressource affectée aux tournées : tracteur, benne, ouvrier saisonnier ou matériel (filets, vibreurs…).
 * Un seul type de document pour toutes (héritage du diagramme de classes → une collection + le champ « type »).
 * Les ouvriers n'ont pas de compte sur la plateforme : ce sont des ressources gérées par la logistique.
 * Collection MongoDB : « ressources ». L'index (type, état) sert les filtres de la liste.
 */
@Document(collection = "ressources")
@CompoundIndex(name = "type_etat", def = "{'type': 1, 'etat': 1}")
@Getter
@Setter
@NoArgsConstructor
public class Ressource {

    @Id
    private String id;

    /** Code métier (TR-01, BN-01, OUV-001, MAT-01), utilisé par les fichiers d'import. */
    @Indexed(unique = true)
    private String code;

    private TypeRessource type;

    /** Modèle du tracteur, nom et prénom de l'ouvrier, désignation du matériel… */
    private String nom;

    /** Immatriculation d'un tracteur ou d'une benne (facultative). */
    private String matricule;

    /** Capacité d'une benne en kilogrammes (obligatoire pour une benne, vide sinon). */
    private Integer capaciteKg;

    /** Nombre d'unités d'un matériel (ex. 40 filets) ; vide pour les autres types. */
    private Integer quantite;

    /** Équipe d'un ouvrier saisonnier (facultative) ; vide pour les autres types. */
    @Indexed(sparse = true)
    private String equipeId;

    /** Téléphone d'un ouvrier (facultatif). */
    private String telephone;

    private EtatRessource etat;

    /** Une ressource archivée n'est plus proposée, mais reste visible dans l'historique des tournées. */
    private boolean archivee;

    @CreatedDate
    private Instant dateCreation;
}
