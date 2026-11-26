package tn.oliveconnect.referentiel;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Une variété telle qu'affichée dans la liste.
 *
 * @param saison période de récolte en toutes lettres, par exemple « Novembre → Février »
 */
public record VarieteReponse(
        String id,
        String code,
        String nom,
        int rendementMoyenKgParHectare,
        int moisDebutRecolte,
        int moisFinRecolte,
        String saison,
        boolean archivee) {

    public static VarieteReponse depuis(Variete variete) {
        return new VarieteReponse(variete.getId(), variete.getCode(), variete.getNom(),
                variete.getRendementMoyenKgParHectare(), variete.getMoisDebutRecolte(), variete.getMoisFinRecolte(),
                nomDuMois(variete.getMoisDebutRecolte()) + " → " + nomDuMois(variete.getMoisFinRecolte()),
                variete.isArchivee());
    }

    private static String nomDuMois(int mois) {
        String nom = Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
        return Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
    }
}
