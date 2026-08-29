package com.fretcorridor.opt.client;

/**
 * Codec polyline encodee (algorithme standard Google/Valhalla, precision 5/1e5).
 *
 * Utile pour agreger une geometrie multi-legs Valhalla : chaque leg de la
 * reponse porte son propre shape (polyline auto-suffisante qui commence a
 * l'origine de ce leg). Concatener naivement deux shapes produirait une
 * polyline invalide (l'encodage est delta-compresse depuis le point de
 * depart de chaque leg) - on decode chaque leg, on concatene les points en
 * evit le point de jonction duplique, puis on re-encode un seul shape
 * continu (plan de reorientation, partie Chauffeur point 3 : multi-legs
 * Valhalla + points d'arret).
 */
final class PolylineUtil {

    private static final double PRECISION = 1e5;

    private PolylineUtil() {
    }

    /** Decode une polyline encodee en liste de {lat, lon} (degres). */
    static double[][] decoder(String encodee) {
        if (encodee == null || encodee.isEmpty()) {
            return new double[0][];
        }
        java.util.List<double[]> points = new java.util.ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;
        int len = encodee.length();
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encodee.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encodee.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lng += dlng;
            points.add(new double[]{lat / PRECISION, lng / PRECISION});
        }
        return points.toArray(new double[0][]);
    }

    /** Encode une liste de {lat, lon} (degres) en polyline. */
    static String encoder(double[][] points) {
        if (points == null || points.length == 0) {
            return "";
        }
        StringBuilder encoded = new StringBuilder();
        long preLat = 0;
        long preLng = 0;
        for (double[] p : points) {
            long lat = Math.round(p[0] * PRECISION);
            long lng = Math.round(p[1] * PRECISION);
            encodeValeur(encoded, lat - preLat);
            encodeValeur(encoded, lng - preLng);
            preLat = lat;
            preLng = lng;
        }
        return encoded.toString();
    }

    private static void encodeValeur(StringBuilder out, long valeur) {
        long v = valeur < 0 ? ~(valeur << 1) : (valeur << 1);
        while (v >= 0x20) {
            out.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        out.append((char) (v + 63));
    }
}
