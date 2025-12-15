package org.bf.mapservice.mapservice.application.query;

import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.GraphHopperClient;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.GraphHopperHttpClient;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.bf.mapservice.mapservice.infrastructure.persistence.ObstacleQueryDao;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteEdgeDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteGeoJsonResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;

@Slf4j
@Service
public class RouteApplicationService {

    private final GraphHopperHttpClient ghClient;

    public RouteApplicationService(GraphHopperHttpClient ghClient) {
        this.ghClient = ghClient;
    }

    public RouteDetailResponseDto findRouteDetail(RouteRequestDto req) {
        String profile = toProfile(req.mobilityType());

        GhRouteResponse res = ghClient.routeGet(
                profile,
                req.startLatitude(), req.startLongitude(),
                req.endLatitude(), req.endLongitude()
        );

        if (res == null || res.paths() == null || res.paths().isEmpty()) {
            throw new IllegalStateException("ROUTE_NOT_FOUND");
        }

        var path = res.paths().get(0);
        var coords = path.points().coordinates();

        // GeoJSON LineString
        RouteGeoJsonResponseDto geo = new RouteGeoJsonResponseDto("LineString", coords);

        // MVP: edges 1개
        List<RouteEdgeDto> edges = List.of(
                new RouteEdgeDto(1, 0L, "unknown", null, path.distance(), false, true, null)
        );

        return new RouteDetailResponseDto(
                path.distance(),
                geo,
                edges,
                true,
                1,
                "NONE"
        );
    }

    private String toProfile(MobilityType mobilityType) {
        return switch (mobilityType) {
            case PEDESTRIAN -> "foot";
            case WHEELCHAIR, STROLLER, ELDERLY -> "wheelchair";
        };
    }
//    public RouteDetailResponseDto findRouteDetail(RouteRequestDto req) {
//        String profile = profileMapper.toGhProfile(req.mobilityType());
//
//        // 1) 장애물 조회용 bbox(출발/도착 기반 + 마진)
//        var bbox = BboxUtil.envelopeWithMarginMeters(
//                req.startLatitude(), req.startLongitude(),
//                req.endLatitude(), req.endLongitude(),
//                800 // 800m 마진 (운영에서 조절)
//        );
//
//        var obstacles = obstacleQueryDao.findActiveObstaclesInEnvelope(
//                bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat(),
//                OffsetDateTime.now()
//        );
//
//        // 2) custom_model 생성
//        var customModel = customModelBuilder.buildCustomModel(obstacles, req.mobilityType(), obstaclePolicy);
//
//        // 3) GH 호출 (custom_model 있으면 ch.disable=true)
//        GhRouteRequest ghReq = new GhRouteRequest(
//                profile,
//                List.of(
//                        List.of(req.startLongitude(), req.startLatitude()),
//                        List.of(req.endLongitude(), req.endLatitude())
//                ),
//                false,
//                false,
//                customModel,
//                Map.of("disable", customModel != null) // custom_model 사용 시 필수 :contentReference[oaicite:5]{index=5}
//        );
//
//        GhRouteResponse res = graphHopperClient.route(ghReq);
//        if (res == null || res.paths() == null || res.paths().isEmpty()) {
//            throw new RuntimeException("ROUTE_NOT_FOUND");
//        }
//
//        var path = res.paths().get(0);
//
//        @SuppressWarnings("unchecked")
//        var points = (Map<String, Object>) path.points();
//        @SuppressWarnings("unchecked")
//        var coords = (List<List<Double>>) points.get("coordinates");
//
//        RouteGeoJsonResponseDto geo = new RouteGeoJsonResponseDto("LineString", coords);
//
//        var edges = List.of(new RouteEdgeDto(1, 0L, "unknown", null, path.distance(), false, true, null));
//
//        return new RouteDetailResponseDto(path.distance(), geo, edges, true, 1, "NONE");
//    }
}