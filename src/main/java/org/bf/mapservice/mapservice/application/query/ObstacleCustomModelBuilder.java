package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.entity.Obstacle;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

import java.util.*;

/*
    DB에서 조회한 Obstacle 목록을 받아서 GraphHopper 라우팅 요청에 넣을수 있는
    areas, priority 로 구성된 Map<String, Object> 형태의 custom_model 만들어줌
*/
@Component
public class ObstacleCustomModelBuilder {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final ObstacleDefaults defaults;

    public ObstacleCustomModelBuilder(ObstacleDefaults defaults) {
        this.defaults = defaults;
    }

    public Map<String, Object> buildCustomModel(List<Obstacle> obstacles, MobilityType mobility, ObstaclePolicy policy) {
        // 배리어프리 여부 판단
        boolean barrierFree = mobility == MobilityType.WHEELCHAIR
                || mobility == MobilityType.STROLLER
                || mobility == MobilityType.ELDERLY;

        // 커스텀 모델 여부
        if ((obstacles == null || obstacles.isEmpty()) && !barrierFree) return null;

        // 결과 담을 맵 LinkedHashMap을 쓴 건 키 순서를 유지하려는 의도
        Map<String, Object> customModel = new LinkedHashMap<>();

        // priority rules GH 커스텀 모델의 “규칙 리스트”
        List<Map<String, Object>> priority = new ArrayList<>();

        // 0) barrierFree 기본 룰: steps 완전 차단(장애물 DB 조회와 무관하게 항상 적용되는 기본 정책)
        if (barrierFree) {
            priority.add(Map.of(
                    "if", "road_class == STEPS",
                    "multiply_by", 0.0
            ));
        }

        // 1) obstacles -> areas + priority rules 장애물이 있으면 areas 생성 + 장애물별 priority 룰 생성
        if (obstacles != null && !obstacles.isEmpty()) {
            customModel.put("areas", buildAreasFeatureCollection(obstacles));

            for (Obstacle o : obstacles) {
                Decision d = policy.decide(mobility, o.getType(), o.getSeverity());

                double mul = d.blocked() ? 0.0 : d.priorityMultiply();

                if (mul >= 1.0) continue;

                priority.add(Map.of(
                        "if", "in_" + areaId(o),
                        "multiply_by", mul
                ));
            }

        }

        if (!priority.isEmpty()) customModel.put("priority", priority);
        return customModel;
    }

    /**
     * GH 문서 스펙: areas 는 GeoJSON FeatureCollection이고 Feature에 id 필드가 있어야 함
     */
    // 2) buildAreasFeatureCollection() 수정: MultiPolygon 분해해서 전부 feature로
    private Map<String, Object> buildAreasFeatureCollection(List<Obstacle> obstacles) {
        List<Map<String, Object>> features = new ArrayList<>();

        for (Obstacle o : obstacles) {
            List<Polygon> polys = toBufferedPolygons(o);
            if (polys.isEmpty()) continue;

            for (int i = 0; i < polys.size(); i++) {
                Polygon poly = polys.get(i);

                Map<String, Object> geometry = Map.of(
                        "type", "Polygon",
                        "coordinates", polygonToGeoJsonCoords(poly)
                );

                Map<String, Object> feature = new LinkedHashMap<>();
                feature.put("type", "Feature");
                // MultiPolygon 분해 시 id가 겹치지 않게 suffix
                feature.put("id", (polys.size() == 1) ? areaId(o) : areaId(o) + "_" + i);
                feature.put("geometry", geometry);
                feature.put("properties", Map.of(
                        "obstacleId", o.getId(),
                        "type", o.getType().name(),
                        "createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null
                ));
                features.add(feature);
            }
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
        Geometry g = o.getGeom();
        if (g == null) return null;

        // radius 보정: DB에 null/0이 들어와도 Defaults 기반으로 의미있는 buffer 생성
        Integer radiusMeters = o.getRadiusMeters();
        int radius =
                (radiusMeters != null && radiusMeters > 0)
                        ? radiusMeters
                        : defaults.defaultRadiusMeters(o.getType());

        if (radius <= 0) {
            // 최종 fallback (서비스에서 이미 보정되지만, 혹시라도 대비)
            radius = (o.getGeomType() == ObstacleGeometryType.POINT) ? 50 : 30;
        }

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
        for (int i = 0; i <= steps; i++) {
            double a = 2.0 * Math.PI * i / steps;
            coords[i] = new Coordinate(
                    center.x + Math.cos(a) * rLon,
                    center.y + Math.sin(a) * rLat
            );
        }
        LinearRing shell = GF.createLinearRing(coords);
        return GF.createPolygon(shell, null);
    }

    private List<List<List<Double>>> polygonToGeoJsonCoords(Polygon p) {
        var ring = p.getExteriorRing().getCoordinates();
        List<List<Double>> outer = new ArrayList<>();
        for (Coordinate c : ring) {
            outer.add(List.of(c.x, c.y));
        }
        return List.of(outer);
    }

    public Map<String, Object> buildAreasOnly(List<Obstacle> obstacles) {
        if (obstacles == null || obstacles.isEmpty()) {
            return Map.of("type", "FeatureCollection", "features", List.of());
        }
        return buildAreasFeatureCollection(obstacles);
    }

    // 3) 기존 toBufferedPolygon() 대신: List<Polygon> 반환으로 변경
    private List<Polygon> toBufferedPolygons(Obstacle o) {
        Geometry g = o.getGeom();
        if (g == null) return List.of();

        Integer radiusMeters = o.getRadiusMeters();
        int radius = (radiusMeters != null && radiusMeters > 0)
                ? radiusMeters
                : defaults.defaultRadiusMeters(o.getType());

        if (radius <= 0) {
            radius = (o.getGeomType() == ObstacleGeometryType.POINT) ? 50 : 30;
        }

        double lat = g.getCoordinate().y;
        double metersPerDegLat = 111_320.0;
        double metersPerDegLon = Math.cos(Math.toRadians(lat)) * 111_320.0;

        double degLat = radius / metersPerDegLat;
        double degLon = radius / metersPerDegLon;

        // POINT는 원(다각형) 1개 반환
        if (o.getGeomType() == ObstacleGeometryType.POINT) {
            Coordinate c = g.getCoordinate();
            return List.of(circlePolygon(c, degLon, degLat, 24));
        }

        // LINESTRING 등은 buffer로 Polygon/MultiPolygon 가능
        double deg = (degLat + degLon) / 2.0;
        Geometry buffered = g.buffer(deg);

        if (buffered instanceof Polygon p) return List.of(p);

        if (buffered instanceof MultiPolygon mp && mp.getNumGeometries() > 0) {
            List<Polygon> out = new ArrayList<>();
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Geometry gi = mp.getGeometryN(i);
                if (gi instanceof Polygon pi) out.add(pi);
            }
            return out;
        }
        return List.of();
    }

}