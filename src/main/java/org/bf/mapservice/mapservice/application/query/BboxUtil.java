package org.bf.mapservice.mapservice.application.query;

public final class BboxUtil {

    public record Bbox(double minLon, double minLat, double maxLon, double maxLat) {}

    private BboxUtil() {}

    public static Bbox envelopeWithMarginMeters(double lat1, double lon1, double lat2, double lon2, double marginMeters) {
        double minLat = Math.min(lat1, lat2);
        double maxLat = Math.max(lat1, lat2);
        double minLon = Math.min(lon1, lon2);
        double maxLon = Math.max(lon1, lon2);

        double midLat = (lat1 + lat2) / 2.0;
        double metersPerDegLat = 111_320.0;
        double metersPerDegLon = Math.cos(Math.toRadians(midLat)) * 111_320.0;

        double dLat = marginMeters / metersPerDegLat;
        double dLon = marginMeters / metersPerDegLon;

        return new Bbox(
                minLon - dLon,
                minLat - dLat,
                maxLon + dLon,
                maxLat + dLat
        );
    }
}