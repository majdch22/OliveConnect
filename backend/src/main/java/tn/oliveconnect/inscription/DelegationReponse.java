package tn.oliveconnect.inscription;

import tn.oliveconnect.utilisateur.Delegation;

/** Une délégation proposée dans la liste du formulaire. */
public record DelegationReponse(Delegation code, String libelle) {

    public static DelegationReponse depuis(Delegation delegation) {
        return new DelegationReponse(delegation, delegation.getLibelle());
    }
}
