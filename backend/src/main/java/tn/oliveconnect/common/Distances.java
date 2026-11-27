package tn.oliveconnect.common;

/** Calculs de distance entre deux points GPS. */
public final class Distances {

    private static final double RAYON_TERRE_METRES = 6_371_000;

    private Distances() {
    }

    /** Distance à vol d'oiseau entre deux points GPS (formule de haversine), en mètres. */
    public static double metres(double latitude1, double longitude1, double latitude2, double longitude2) {
        double ecartLatitude = Math.toRadians(latitude2 - latitude1);
        double ecartLongitude = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(ecartLatitude / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.pow(Math.sin(ecartLongitude / 2), 2);
        return 2 * RAYON_TERRE_METRES * Math.asin(Math.sqrt(a));
    }
}
