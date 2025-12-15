package org.bf.mapservice.mapservice.application.query;

import lombok.extern.slf4j.Slf4j;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.GraphHopperHttpClient;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteEdgeDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteGeoJsonResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RouteApplicationService {

    private final GraphHopperHttpClient ghClient;

    public RouteApplicationService(GraphHopperHttpClient ghClient) {
        this.ghClient = ghClient;
    }

    public RouteDetailResponseDto findRouteDetail(RouteRequestDto req) {

        String profile = toProfile(req.mobilityType());

        boolean barrierFree =
                req.mobilityType() == MobilityType.WHEELCHAIR
                        || req.mobilityType() == MobilityType.STROLLER
                        || req.mobilityType() == MobilityType.ELDERLY;

        Map<String, Object> customModel = null;
        Map<String, Object> ch = Map.of("disable", false);

        if (barrierFree) {
            customModel = Map.of(
                    "priority", List.of(
                            Map.of(
                                    "if", "road_class == STEPS",
                                    "multiply_by", 0
                            )
                    )
            );
            ch = Map.of("disable", true);
        }

        GhRouteRequest ghReq = new GhRouteRequest(
                profile,
                List.of(
                        List.of(req.startLongitude(), req.startLatitude()),
                        List.of(req.endLongitude(), req.endLatitude())
                ),
                false,   // points_encoded
                false,   // instructions (turn-by-turn 필요 없으니 false)
                customModel,
                ch
        );

        GhRouteResponse res = ghClient.routePost(ghReq);

        if (res == null || res.paths() == null || res.paths().isEmpty()) {
            throw new IllegalStateException("ROUTE_NOT_FOUND");
        }

        var path = res.paths().get(0);
        var coords = path.points().coordinates();

        RouteGeoJsonResponseDto geo =
                new RouteGeoJsonResponseDto("LineString", coords);

        var edges = List.of(
                new RouteEdgeDto(1, 0L, "unknown", null,
                        path.distance(), false, true, null)
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
}