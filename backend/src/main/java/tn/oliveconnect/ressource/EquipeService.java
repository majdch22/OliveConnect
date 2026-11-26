package tn.oliveconnect.ressource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tn.oliveconnect.common.exception.ErreurMetierException;
import tn.oliveconnect.compteur.CompteurService;

/**
 * Gestion des équipes d'ouvriers saisonniers.
 * Une équipe ne peut être archivée que si elle n'a plus de membres (les ouvriers sont d'abord changés d'équipe).
 */
@Service
@RequiredArgsConstructor
public class EquipeService {

    /**
     * Une équipe et son effectif.
     *
     * @param nombreDisponibles ouvriers de l'équipe disponibles aujourd'hui (ex. « 7 sur 8 »)
     */
    public record EquipeReponse(String id, String code, String nom, long nombreMembres, long nombreDisponibles,
            boolean archivee) {
    }

    /** Formulaire d'une équipe : seul le nom est saisi, le code est attribué par la plateforme. */
    public record EquipeRequete(
            @NotBlank(message = "Saisissez le nom de l'équipe.")
            @Size(max = 40, message = "40 caractères au maximum.")
            String nom) {
    }

    private final EquipeRepository equipeRepository;
    private final RessourceRepository ressourceRepository;
    private final RessourceService ressourceService;
    private final CompteurService compteurService;

    public List<EquipeReponse> lister(boolean avecArchives) {
        return equipeRepository.findAll().stream()
                .filter(equipe -> avecArchives || !equipe.isArchivee())
                .sorted(Comparator.comparing(Equipe::getCode))
                .map(this::reponse)
                .toList();
    }

    public List<RessourceReponse> membres(String id) {
        trouver(id);
        return ressourceService.membres(id);
    }

    public EquipeReponse creer(EquipeRequete requete) {
        verifierNomLibre(requete.nom(), null);
        Equipe equipe = new Equipe();
        equipe.setCode(compteurService.prochainCode("EQUIPE", "EQ-%02d"));
        equipe.setNom(requete.nom().trim());
        return reponse(equipeRepository.save(equipe));
    }

    public EquipeReponse renommer(String id, EquipeRequete requete) {
        Equipe equipe = trouver(id);
        verifierNomLibre(requete.nom(), id);
        equipe.setNom(requete.nom().trim());
        return reponse(equipeRepository.save(equipe));
    }

    public EquipeReponse changerArchivage(String id, boolean archivee) {
        Equipe equipe = trouver(id);
        if (archivee && ressourceRepository.countByEquipeIdAndArchiveeFalse(id) > 0) {
            throw new ErreurMetierException(HttpStatus.CONFLICT, "EQUIPE_NON_VIDE",
                    "Cette équipe a encore des ouvriers : changez-les d'équipe avant de l'archiver.");
        }
        equipe.setArchivee(archivee);
        return reponse(equipeRepository.save(equipe));
    }

    private EquipeReponse reponse(Equipe equipe) {
        return new EquipeReponse(equipe.getId(), equipe.getCode(), equipe.getNom(),
                ressourceRepository.countByEquipeIdAndArchiveeFalse(equipe.getId()),
                ressourceRepository.countByEquipeIdAndEtatAndArchiveeFalse(equipe.getId(), EtatRessource.DISPONIBLE),
                equipe.isArchivee());
    }

    private void verifierNomLibre(String nom, String equipeModifiee) {
        equipeRepository.findByNomIgnoreCase(nom.trim())
                .filter(autre -> !Objects.equals(autre.getId(), equipeModifiee))
                .ifPresent(autre -> {
                    throw new ErreurMetierException(HttpStatus.CONFLICT, "NOM_DEJA_UTILISE",
                            "Une équipe porte déjà ce nom.", Map.of("nom", "Une équipe porte déjà ce nom."));
                });
    }

    private Equipe trouver(String id) {
        return equipeRepository.findById(id).orElseThrow(() ->
                new ErreurMetierException(HttpStatus.NOT_FOUND, "EQUIPE_INTROUVABLE", "Cette équipe n'existe pas."));
    }
}
