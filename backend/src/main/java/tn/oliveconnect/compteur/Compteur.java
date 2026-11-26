package tn.oliveconnect.compteur;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Compteur servant à fabriquer des numéros lisibles (ADH-0042, D-118, T-42…).
 * MongoDB n'a pas de numérotation automatique : un document par type de numéro garde la dernière valeur donnée.
 * Collection MongoDB : « compteurs ».
 */
@Document(collection = "compteurs")
@Getter
@NoArgsConstructor
public class Compteur {

    /** Nom du compteur, par exemple « ADHERENT ». */
    @Id
    private String nom;

    /** Dernière valeur attribuée. */
    private long valeur;
}
