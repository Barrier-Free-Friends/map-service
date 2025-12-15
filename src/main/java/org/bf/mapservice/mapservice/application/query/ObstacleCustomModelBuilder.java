package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.entity.Obstacle;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ObstacleCustomModelBuilder {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    public Map<String, Object> buildCustomModel(List<Obstacle> obstacles, MobilityType mobility, ObstaclePolicy policy) {
        if (obstacles == null || obstacles.isEmpty()) return null;

        // 1) areas: FeatureCollection
        Map<String, Object> areas = buildAreas(obstacles);

        // 2) priority rules: in_area('obst_{id}')이면 multiply_by 적용
        List<Map<String, Object>> priority = obstacles.stream()
                .map(o -> {
                    var d = policy.decide(mobility, o.getType(), o.getSeverity());
                    if (d.priorityMultiply() >= 1.0) return null;

                    String areaName = areaName(o);
                    return Map.<String, Object>of(
                            "if", "in_area('" + areaName + "')",
                            "multiply_by", String.valueOf(d.priorityMultiply())
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> customModel = new LinkedHashMap<>();
        customModel.put("areas", areas);
        if (!priority.isEmpty()) customModel.put("priority", priority);

        return customModel;
    }

    private Map<String, Object> buildAreas(List<Obstacle> obstacles) {
        List<Map<String, Object>> features = new ArrayList<>();

        for (Obstacle o : obstacles) {
            Polygon poly = toBufferedPolygon(o);
            if (poly == null) continue;

            Map<String, Object> geometry = Map.of(
                    "type", "Polygon",
                    "coordinates", polygonToGeoJsonCoords(poly)
            );

            Map<String, Object> properties = Map.of(
                    "id", o.getId(),
                    "name", areaName(o)
            );

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("properties", properties);
            feature.put("geometry", geometry);

            features.add(feature);
        }

        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        return fc;
    }

    private String areaName(Obstacle o) {
        return "obst_" + o.getId();
    }

    /**
     * MVP용 버퍼 폴리곤:
     * - POINT: radiusMeters(없으면 8m) 원형 근사(24각형)
     * - LINESTRING: (radiusMeters 없으면 6m) 선 주변 버퍼(단순 근사: JTS buffer를 degree 변환으로 사용)
     *
     * 정밀도가 더 필요하면: DB에서 geography buffer로 GeoJSON 폴리곤을 만들어서 넘기는 방식으로 고도화 권장.
     */
    private Polygon toBufferedPolygon(Obstacle o) {
        int radius = (o.getRadiusMeters() != null) ? o.getRadiusMeters() : (o.getGeomType() == ObstacleGeometryType.POINT ? 8 : 6);

        Geometry g = o.getGeom();
        if (g == null) return null;

        // meters -> degrees 근사 (lat 기준)
        double lat = g.getCoordinate().y;
        double metersPerDegLat = 111_320.0;
        double metersPerDegLon = Math.cos(Math.toRadians(lat)) * 111_320.0;

        double degLat = radius / metersPerDegLat;
        double degLon = radius / metersPerDegLon;

        if (o.getGeomType() == ObstacleGeometryType.POINT) {
            Coordinate c = g.getCoordinate();
            return circlePolygon(c, degLon, degLat, 24);
        }

        // LINESTRING: JTS buffer는 degree 단위이므로 평균 deg 사용
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
        // GeoJSON Polygon: [ [ [lon,lat], [lon,lat], ... ] ] (outer ring only MVP)
        var ring = p.getExteriorRing().getCoordinates();
        List<List<Double>> outer = new ArrayList<>();
        for (Coordinate c : ring) {
            outer.add(List.of(c.x, c.y));
        }
        return List.of(outer);
    }
}