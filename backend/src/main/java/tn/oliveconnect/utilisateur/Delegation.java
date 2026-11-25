package tn.oliveconnect.utilisateur;

/** Délégations du gouvernorat de Sfax, zone d'activité de la coopérative. */
public enum Delegation {

    SFAX_VILLE("Sfax Ville"),
    SFAX_OUEST("Sfax Ouest"),
    SFAX_SUD("Sfax Sud"),
    SAKIET_EZZIT("Sakiet Ezzit"),
    SAKIET_EDDAIER("Sakiet Eddaier"),
    THYNA("Thyna"),
    AGAREB("Agareb"),
    JEBINIANA("Jebiniana"),
    EL_HENCHA("El Hencha"),
    MENZEL_CHAKER("Menzel Chaker"),
    GHRAIBA("Ghraïba"),
    BIR_ALI_BEN_KHALIFA("Bir Ali Ben Khalifa"),
    SKHIRA("Skhira"),
    MAHRES("Mahrès"),
    KERKENNAH("Kerkennah"),
    EL_AMRA("El Amra");

    private final String libelle;

    Delegation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
