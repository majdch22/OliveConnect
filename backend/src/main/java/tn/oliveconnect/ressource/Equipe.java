package tn.oliveconnect.ressource;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Équipe d'ouvriers saisonniers, affectée en un clic à une tournée.
 * Les membres sont les ressources « ouvrier » qui portent l'identifiant de l'équipe (référence côté ouvrier).
 * Collection MongoDB : « equipes ».
 */
@Document(collection = "equipes")
@Getter
@Setter
@NoArgsConstructor
public class Equipe {

    @Id
    private String id;

    /** Code métier (EQ-01…). */
    @Indexed(unique = true)
    private String code;

    @Indexed(unique = true)
    private String nom;

    private boolean archivee;

    @CreatedDate
    private Instant dateCreation;
}
