package org.bf.mapservice.mapservice.application.service;

import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.domain.entity.MobilityProfile;
import org.bf.mapservice.mapservice.infrastructure.persistence.RoutingRepository;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RouteApplicationService {

    private final RoutingRepository routingRepository;

    public RouteApplicationService(RoutingRepository routingRepository) {
        this.routingRepository = routingRepository;
    }

    /**
     * pgRouting 기반 경로 탐색
     * - 입력: 위도/경도 + MobilityType
     * - 출력: 지도에 바로 쓸 수 있는 (lat, lon) 리스트
     */
    public List<RoutePointDto> findRoute(FindRouteQuery query) {
        MobilityProfile profile = MobilityProfile.from(query.mobilityType());

        // 1) 시작/끝 vertex 찾기
        long startVertex = routingRepository.findNearestVertex(
                query.startLatitude(),
                query.startLongitude()
        );
        long endVertex = routingRepository.findNearestVertex(
                query.endLatitude(),
                query.endLongitude()
        );

        // 2) MobilityProfile -> 사용할 ways_* 뷰 이름
        String waysView = resolveWaysViewName(profile);

        // 3) pgRouting으로 좌표 포함 경로 찾기
        List<RoutePointDto> routePoints = routingRepository.findRoutePoints(
                startVertex,
                endVertex,
                waysView
        );

        if (routePoints.isEmpty()) {
            throw new IllegalStateException(
                    "경로를 찾지 못했습니다. startVertex=%d, endVertex=%d, waysView=%s"
                            .formatted(startVertex, endVertex, waysView)
            );
        }

        return routePoints;
    }

    private String resolveWaysViewName(MobilityProfile profile) {
        // 추후: profile에 따라 ways_wheelchair, ways_stroller 등으로 분기
        return "ways_walk";
    }
}
