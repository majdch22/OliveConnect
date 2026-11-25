package tn.oliveconnect.utilisateur;

/** État d'un compte. Seul un compte ACTIF peut se connecter. */
public enum StatutCompte {

    /** Agriculteur inscrit, en attente de vérification par la coopérative. */
    EN_ATTENTE_VALIDATION,

    /** Compte utilisable. */
    ACTIF,

    /** Inscription refusée par l'administrateur. */
    REFUSE,

    /** Compte coupé par l'administrateur. */
    DESACTIVE
}
