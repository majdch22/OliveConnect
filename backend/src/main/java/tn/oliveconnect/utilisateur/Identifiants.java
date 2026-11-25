package tn.oliveconnect.utilisateur;

/**
 * Met l'e-mail et le téléphone sous une forme unique avant l'enregistrement ou la recherche,
 * pour que « 98 123 412 » et « +21698123412 » désignent le même compte.
 */
public final class Identifiants {

    private static final String INDICATIF_TUNISIE = "+216";

    private Identifiants() {
    }

    /** Vrai si l'identifiant saisi à la connexion est un e-mail plutôt qu'un téléphone. */
    public static boolean estEmail(String identifiant) {
        return identifiant != null && identifiant.contains("@");
    }

    /** E-mail sans espaces et en minuscules. */
    public static String normaliserEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Téléphone sans espaces ni séparateurs ; un numéro tunisien à 8 chiffres reçoit l'indicatif +216. */
    public static String normaliserTelephone(String telephone) {
        if (telephone == null) {
            return null;
        }
        String chiffres = telephone.replaceAll("[\\s.\\-()]", "");
        if (chiffres.startsWith("00216")) {
            return INDICATIF_TUNISIE + chiffres.substring(5);
        }
        if (chiffres.matches("\\d{8}")) {
            return INDICATIF_TUNISIE + chiffres;
        }
        return chiffres;
    }
}
