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


        // 1) bbox 장애물 조회 범위 만들기
        var bbox = BboxUtil.envelopeWithMarginMeters(
                req.startLatitude(), req.startLongitude(),
                req.endLatitude(), req.endLongitude(),
                800
        );

        // bbox 안의 “활성 장애물” 조회
        var obstacles = obstacleQueryDaoImpl.findActiveObstaclesInEnvelope(
                bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat(),
                OffsetDateTime.now()
        );

        // 2) custom_model 생성
        var customModel = obstacleCustomModelBuilder.buildCustomModel(
                obstacles, req.mobilityType(), obstaclePolicy
        );

        // custom_model 사용 여부와 profile 결정
        boolean useCustom = (customModel != null);
        String profile = toProfile(req.mobilityType(), useCustom);

        // GraphHopper 요청 DTO 만들기
        GhRouteRequest ghReq = new GhRouteRequest(
                profile,
                List.of(
                        List.of(req.startLongitude(), req.startLatitude()),
                        List.of(req.endLongitude(), req.endLatitude())
                ),
                false,
                false,
                customModel,
                useCustom,   // ch.disable
                useCustom    // lm.disable (필요 없으면 false로)
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

        boolean barrierFree = req.mobilityType().isBarrierSensitive();

        boolean fullyAccessible = true;
        String firstBlockedReason =  "NONE";

        if(obstacles != null){
            for (var o : obstacles){
                Decision d = obstaclePolicy.decide(req.mobilityType(), o.getType(), o.getSeverity());
                if(d.blocked() || d.priorityMultiply() <= 0.0){
                    fullyAccessible = false;
                    firstBlockedReason =o.getType().name();
                    break;
                }
            }
        }

        Integer accessibleUntilSeq = fullyAccessible ? null : 0;

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