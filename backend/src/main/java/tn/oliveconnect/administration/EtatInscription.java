package tn.oliveconnect.administration;

import java.util.Set;

import tn.oliveconnect.utilisateur.StatutCompte;

/** Les trois onglets de la page des inscriptions, et les états de compte qu'ils regroupent. */
public enum EtatInscription {

    EN_ATTENTE(Set.of(StatutCompte.EN_ATTENTE_VALIDATION)),

    /** Une inscription acceptée reste dans cet onglet même si le compte a été désactivé plus tard. */
    ACCEPTEE(Set.of(StatutCompte.ACTIF, StatutCompte.DESACTIVE)),

    REFUSEE(Set.of(StatutCompte.REFUSE));

    private final Set<StatutCompte> statuts;

    EtatInscription(Set<StatutCompte> statuts) {
        this.statuts = statuts;
    }

    public Set<StatutCompte> getStatuts() {
        return statuts;
    }
}
