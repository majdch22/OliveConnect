package tn.oliveconnect.utilisateur;

/**
 * Délégations du gouvernorat de Sfax, zone d'activité de la coopérative.
 * Chaque délégation a un point central approximatif, utilisé pour la placer sur la carte.
 */
public enum Delegation {

    SFAX_VILLE("Sfax Ville", 34.740, 10.760),
    SFAX_OUEST("Sfax Ouest", 34.745, 10.690),
    SFAX_SUD("Sfax Sud", 34.690, 10.680),
    SAKIET_EZZIT("Sakiet Ezzit", 34.810, 10.770),
    SAKIET_EDDAIER("Sakiet Eddaier", 34.800, 10.860),
    THYNA("Thyna", 34.660, 10.700),
    AGAREB("Agareb", 34.740, 10.530),
    JEBINIANA("Jebiniana", 35.035, 10.910),
    EL_HENCHA("El Hencha", 35.130, 10.740),
    MENZEL_CHAKER("Menzel Chaker", 34.970, 10.370),
    GHRAIBA("Ghraïba", 34.530, 10.200),
    BIR_ALI_BEN_KHALIFA("Bir Ali Ben Khalifa", 34.735, 10.100),
    SKHIRA("Skhira", 34.300, 10.070),
    MAHRES("Mahrès", 34.530, 10.500),
    KERKENNAH("Kerkennah", 34.710, 11.180),
    EL_AMRA("El Amra", 35.000, 10.920);

    private final String libelle;
    private final double latitude;
    private final double longitude;

    Delegation(String libelle, double latitude, double longitude) {
        this.libelle = libelle;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLibelle() {
        return libelle;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
