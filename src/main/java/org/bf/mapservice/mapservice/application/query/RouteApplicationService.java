package org.bf.mapservice.mapservice.application.query;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.GraphHopperHttpClient;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.bf.mapservice.mapservice.infrastructure.persistence.ObstacleQueryDaoImpl;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteEdgeDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteGeoJsonResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteApplicationService {

    private final GraphHopperHttpClient ghClient;
    private final ObstacleCustomModelBuilder obstacleCustomModelBuilder;
    private final ObstacleQueryDaoImpl obstacleQueryDaoImpl;
    private final ObstaclePolicy obstaclePolicy;

    public RouteDetailResponseDto findRouteDetail(RouteRequestDto req) {

        // 1) bbox
        var bbox = BboxUtil.envelopeWithMarginMeters(
                req.startLatitude(), req.startLongitude(),
                req.endLatitude(), req.endLongitude(),
                800
        );

        var obstacles = obstacleQueryDaoImpl.findActiveObstaclesInEnvelope(
                bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat(),
                OffsetDateTime.now()
        );

        // 2) custom_model
        var customModel = obstacleCustomModelBuilder.buildCustomModel(
                obstacles, req.mobilityType(), obstaclePolicy
        );

        boolean useCustom = (customModel != null);
        String profile = toProfile(req.mobilityType(), useCustom);

        GhRouteRequest ghReq = new GhRouteRequest(
                profile,
                List.of(
                        List.of(req.startLongitude(), req.startLatitude()),
                        List.of(req.endLongitude(), req.endLatitude())
                ),
                false,
                false,
                customModel,
                Map.of("disable", useCustom)
        );

        GhRouteResponse res = ghClient.routePost(ghReq);

        if (res == null || res.paths() == null || res.paths().isEmpty()) {
            throw new IllegalStateException("ROUTE_NOT_FOUND");
        }

        var path = res.paths().get(0);
        var coords = path.points().coordinates();

        RouteGeoJsonResponseDto geo =
                new RouteGeoJsonResponseDto("LineString", coords);

        // 🔹 MVP: edge는 아직 단일
        var edges = List.of(
                new RouteEdgeDto(
                        1,
                        0L,
                        "route",
                        null,
                        path.distance(),
                        false,
                        true,
                        null
                )
        );

        boolean barrierFree =
                req.mobilityType() == MobilityType.WHEELCHAIR
                        || req.mobilityType() == MobilityType.STROLLER
                        || req.mobilityType() == MobilityType.ELDERLY;

        boolean fullyAccessible =
                !(barrierFree && obstacles != null && !obstacles.isEmpty());

        Integer accessibleUntilSeq =
                fullyAccessible ? null : 0;

        String firstBlockedReason =
                fullyAccessible ? "NONE" : "STAIRS";

        return new RouteDetailResponseDto(
                path.distance(),
                geo,
                edges,
                fullyAccessible,
                accessibleUntilSeq,
                firstBlockedReason,
                req.mobilityType().name()
        );
    }

    private String toProfile(MobilityType mobilityType, boolean useCustom) {
        return switch (mobilityType) {
            case PEDESTRIAN -> useCustom ? "foot_custom" : "foot";
            case WHEELCHAIR, STROLLER, ELDERLY -> "wheelchair"; // wheelchair은 이미 custom weighting이니까 그대로 OK
        };
    }
}