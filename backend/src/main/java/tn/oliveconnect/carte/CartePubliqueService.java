package tn.oliveconnect.carte;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.oliveconnect.alerte.Alerte;
import tn.oliveconnect.alerte.AlerteRepository;
import tn.oliveconnect.alerte.StatutAlerte;
import tn.oliveconnect.utilisateur.Role;
import tn.oliveconnect.utilisateur.StatutCompte;
import tn.oliveconnect.utilisateur.Utilisateur;
import tn.oliveconnect.utilisateur.UtilisateurRepository;
import tn.oliveconnect.verger.StatutRecolte;
import tn.oliveconnect.verger.Verger;
import tn.oliveconnect.verger.VergerRepository;

/**
 * Vergers de la carte publique (visiteurs, sans connexion).
 * Respect de la vie privée : ni le nom de l'agriculteur, ni le code du verger, ni la description des signalements ;
 * la position est arrondie à environ 100 m. Seuls les vergers non archivés des adhérents actifs y figurent.
 */
@Service
@RequiredArgsConstructor
public class CartePubliqueService {

    private final VergerRepository vergerRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AlerteRepository alerteRepository;

    /** Un verger tel que le voit un visiteur. */
    public record VergerPublic(String nom, String libelleDelegation, String nomVariete, double superficieHa,
            Integer niveauMaturite, StatutRecolte statutRecolte, String libelleStatutRecolte, boolean alerteEnCours,
            double latitude, double longitude) {
    }

    public List<VergerPublic> vergers() {
        List<String> adherentsActifs = utilisateurRepository.findByRoleAndStatut(Role.AGRICULTEUR, StatutCompte.ACTIF).stream()
                .map(Utilisateur::getId).toList();
        Set<String> vergersEnAlerte = alerteRepository.findByStatutIn(StatutAlerte.NON_RESOLUS).stream()
                .map(Alerte::getVergerId).collect(Collectors.toSet());
        return vergerRepository.findByArchiveFalseAndAgriculteurIdIn(adherentsActifs).stream()
                .map(verger -> versPublic(verger, vergersEnAlerte.contains(verger.getId())))
                .toList();
    }

    private static VergerPublic versPublic(Verger verger, boolean alerteEnCours) {
        return new VergerPublic(verger.getNom(), verger.getDelegation().getLibelle(), verger.getNomVariete(),
                verger.getSuperficieHa(), verger.getNiveauMaturite(), verger.getStatutRecolte(),
                verger.getStatutRecolte().getLibelle(), alerteEnCours,
                // GeoJSON range les coordonnées dans l'ordre [longitude, latitude]
                arrondir(verger.getLocalisation().getY()), arrondir(verger.getLocalisation().getX()));
    }

    /** Trois décimales : environ 100 m, assez pour situer un verger sans désigner une parcelle précise. */
    private static double arrondir(double coordonnee) {
        return Math.round(coordonnee * 1000) / 1000.0;
    }
}
