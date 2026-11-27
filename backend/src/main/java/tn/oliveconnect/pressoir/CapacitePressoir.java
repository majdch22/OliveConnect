package tn.oliveconnect.pressoir;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Capacité de traitement d'un pressoir pour un jour donné, publiée par son responsable (décision 7)
 * quand elle diffère de la capacité habituelle (maintenance, panne, journée renforcée…).
 * Collection MongoDB : « capacites_pressoirs », un document par pressoir et par jour (index unique).
 * Le réservé (tournées prévues) et le reçu (pesées) ne sont pas rangés ici : ils sont calculés.
 */
@Document(collection = "capacites_pressoirs")
@CompoundIndex(name = "pressoir_date", def = "{'pressoirId': 1, 'date': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
public class CapacitePressoir {

    @Id
    private String id;

    private String pressoirId;
    private LocalDate date;
    private int maximumKg;
    private String motif;

    private String auteurId;
    private String nomAuteur;
    private Instant dateModification;
}
