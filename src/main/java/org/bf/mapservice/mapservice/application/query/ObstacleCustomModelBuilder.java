package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.entity.Obstacle;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ObstacleCustomModelBuilder {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    public Map<String, Object> buildCustomModel(List<Obstacle> obstacles, MobilityType mobility, ObstaclePolicy policy) {
        boolean barrierFree = mobility == MobilityType.WHEELCHAIR
                || mobility == MobilityType.STROLLER
                || mobility == MobilityType.ELDERLY;

        if ((obstacles == null || obstacles.isEmpty()) && !barrierFree) return null;

        Map<String, Object> customModel = new LinkedHashMap<>();

        // priority rules
        List<Map<String, Object>> priority = new ArrayList<>();

        // 0) barrierFree 기본 룰: steps 완전 차단
        if (barrierFree) {
            priority.add(Map.of(
                    "if", "road_class == STEPS",
                    "multiply_by", 0.0
            ));
        }

        // 1) obstacles -> areas + priority rules
        if (obstacles != null && !obstacles.isEmpty()) {
            customModel.put("areas", buildAreasFeatureCollection(obstacles));

            for (Obstacle o : obstacles) {
                var d = policy.decide(mobility, o.getType(), o.getSeverity());
                if (d.priorityMultiply() >= 1.0) continue;

                // GH 문서: areas id = "obst_1" 이면 조건은 "in_obst_1"
                priority.add(Map.of(
                        "if", "in_" + areaId(o),
                        "multiply_by", d.priorityMultiply()
                ));
            }
        }

        if (!priority.isEmpty()) customModel.put("priority", priority);
        return customModel;
    }

    /**
     * GH 문서 스펙: areas 는 GeoJSON FeatureCollection이고 Feature에 id 필드가 있어야 함
     */
    private Map<String, Object> buildAreasFeatureCollection(List<Obstacle> obstacles) {
        List<Map<String, Object>> features = new ArrayList<>();

        for (Obstacle o : obstacles) {
            Polygon poly = toBufferedPolygon(o);
            if (poly == null) continue;

            Map<String, Object> geometry = Map.of(
                    "type", "Polygon",
                    "coordinates", polygonToGeoJsonCoords(poly)
            );

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("id", areaId(o));      // 중요: "obst_1"
            feature.put("geometry", geometry);  // properties 필요 없음

            features.add(feature);
        }

        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        return fc;
    }

    private String areaId(Obstacle o) {
        return "obst_" + o.getId();
    }

    private Polygon toBufferedPolygon(Obstacle o) {
        int radius = (o.getRadiusMeters() != null)
                ? o.getRadiusMeters()
                : (o.getGeomType() == ObstacleGeometryType.POINT ? 8 : 6);

        Geometry g = o.getGeom();
        if (g == null) return null;

        double lat = g.getCoordinate().y;
        double metersPerDegLat = 111_320.0;
        double metersPerDegLon = Math.cos(Math.toRadians(lat)) * 111_320.0;

        double degLat = radius / metersPerDegLat;
        double degLon = radius / metersPerDegLon;

        if (o.getGeomType() == ObstacleGeometryType.POINT) {
            Coordinate c = g.getCoordinate();
            return circlePolygon(c, degLon, degLat, 24);
        }

        double deg = (degLat + degLon) / 2.0;
        Geometry buffered = g.buffer(deg);

        if (buffered instanceof Polygon p) return p;
        if (buffered instanceof MultiPolygon mp && mp.getNumGeometries() > 0) return (Polygon) mp.getGeometryN(0);
        return null;
    }

    private Polygon circlePolygon(Coordinate center, double rLon, double rLat, int steps) {
        Coordinate[] coords = new Coordinate[steps + 1];
        for (int i = 0; i < steps; i++) {
            double ang = (2.0 * Math.PI) * i / steps;
            coords[i] = new Coordinate(
                    center.x + (Math.cos(ang) * rLon),
                    center.y + (Math.sin(ang) * rLat)
            );
        }
        coords[steps] = coords[0];
        LinearRing ring = GF.createLinearRing(coords);
        return GF.createPolygon(ring);
    }

    private List<List<List<Double>>> polygonToGeoJsonCoords(Polygon p) {
        var ring = p.getExteriorRing().getCoordinates();
        List<List<Double>> outer = new ArrayList<>();
        for (Coordinate c : ring) {
            outer.add(List.of(c.x, c.y));
        }
        return List.of(outer);
    }
}