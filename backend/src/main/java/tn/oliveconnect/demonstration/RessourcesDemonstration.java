package tn.oliveconnect.demonstration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.ressource.EquipeService;
import tn.oliveconnect.ressource.EquipeService.EquipeRequete;
import tn.oliveconnect.ressource.EtatRessource;
import tn.oliveconnect.ressource.RessourceRepository;
import tn.oliveconnect.ressource.RessourceRequete;
import tn.oliveconnect.ressource.RessourceService;
import tn.oliveconnect.ressource.TypeRessource;

/**
 * Ressources de démonstration, créées seulement en développement (profil « dev ») et seulement si la collection est vide :
 * tracteurs, bennes, deux équipes d'ouvriers, quelques ouvriers sans équipe et du matériel de récolte.
 */
@Component
@Profile("dev")
@Order(4)
@RequiredArgsConstructor
public class RessourcesDemonstration implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(RessourcesDemonstration.class);

    private final RessourceRepository ressourceRepository;
    private final RessourceService ressourceService;
    private final EquipeService equipeService;

    @Override
    public void run(ApplicationArguments arguments) {
        if (ressourceRepository.count() > 0) {
            return;
        }
        // Tracteurs : l'un est en panne, un autre en révision
        tracteur("New Holland TD5", "4521 TU 210", EtatRessource.DISPONIBLE);
        tracteur("Massey Ferguson 375", "3310 TU 198", EtatRessource.DISPONIBLE);
        tracteur("John Deere 5075E", "6102 TU 221", EtatRessource.EN_PANNE);
        tracteur("Kubota M7060", "7014 TU 224", EtatRessource.DISPONIBLE);
        tracteur("Same Explorer 90", "2287 TU 185", EtatRessource.EN_REVISION);
        tracteur("Landini 5-100", "5590 TU 216", EtatRessource.DISPONIBLE);

        // Bennes de 2 et 3 tonnes
        for (int numero = 1; numero <= 9; numero++) {
            int capacite = numero % 3 == 0 ? 3000 : 2000;
            EtatRessource etat = numero == 7 ? EtatRessource.EN_REVISION : EtatRessource.DISPONIBLE;
            ressourceService.creer(new RessourceRequete(TypeRessource.BENNE, "Benne " + capacite / 1000 + " t",
                    "BEN " + (100 + numero), capacite, null, null, null, etat));
        }

        // Deux équipes d'ouvriers saisonniers
        String equipeA = equipeService.creer(new EquipeRequete("Équipe A")).id();
        String equipeB = equipeService.creer(new EquipeRequete("Équipe B")).id();
        List.of("Youssef Hammami", "Bilel Gharbi", "Anis Trabelsi", "Hichem Ayari", "Mohamed Jebali",
                "Sofiene Dridi", "Marwen Chaari", "Nabil Ferchichi")
                .forEach(nom -> ouvrier(nom, equipeA, EtatRessource.DISPONIBLE));
        List.of("Faouzi Ben Amor", "Ridha Mansouri", "Karim Saidi", "Lotfi Bouaziz", "Slim Kallel", "Ameur Masmoudi")
                .forEach(nom -> ouvrier(nom, equipeB, nom.startsWith("Slim") ? EtatRessource.EN_CONGE : EtatRessource.DISPONIBLE));
        ouvrier("Walid Hamdi", null, EtatRessource.DISPONIBLE);
        ouvrier("Tarek Zouari", null, EtatRessource.EN_CONGE);

        // Matériel de récolte
        materiel("Filets de récolte 10 × 8 m", 40);
        materiel("Peignes vibreurs électriques", 6);
        materiel("Caisses de transport", 320);

        journal.info("Ressources de démonstration créées : {} ressources, 2 équipes", ressourceRepository.count());
    }

    private void tracteur(String nom, String matricule, EtatRessource etat) {
        ressourceService.creer(new RessourceRequete(TypeRessource.TRACTEUR, nom, matricule, null, null, null, null, etat));
    }

    private void ouvrier(String nom, String equipeId, EtatRessource etat) {
        ressourceService.creer(new RessourceRequete(TypeRessource.OUVRIER, nom, null, null, null, equipeId, null, etat));
    }

    private void materiel(String nom, int quantite) {
        ressourceService.creer(new RessourceRequete(TypeRessource.MATERIEL, nom, null, null, quantite, null, null,
                EtatRessource.DISPONIBLE));
    }
}
