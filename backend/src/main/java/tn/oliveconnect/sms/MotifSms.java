package tn.oliveconnect.sms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Raison de l'envoi d'un SMS. */
@Getter
@RequiredArgsConstructor
public enum MotifSms {
    INSCRIPTION_ACCEPTEE("Inscription acceptée"),
    INSCRIPTION_REFUSEE("Inscription refusée"),
    CODE_MOT_DE_PASSE("Code de mot de passe oublié"),
    COLLECTE("Suivi d'une collecte");

    private final String libelle;
}
