package org.bf.mapservice.mapservice.application.service;

import lombok.AllArgsConstructor;
import org.bf.global.infrastructure.exception.CustomException;
import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.domain.entity.MobilityProfile;
import org.bf.mapservice.mapservice.domain.exception.MapErrorCode;
import org.bf.mapservice.mapservice.infrastructure.persistence.RoutingRepository;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class RouteApplicationService {

    private final RoutingRepository routingRepository;

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
        String waysView = resolveWaysViewName(profile);  // 예: ways_wheelchair, ways_stroller, ways_walk ...

        // 3) 1차: 요청한 mobility 프로필 기준으로 경로 찾기
        List<RoutePointDto> routePoints = routingRepository.findRoutePoints(
                startVertex,
                endVertex,
                waysView
        );

        if (!routePoints.isEmpty()) {
            //프로필 기준으로도 경로를 찾았다 → 정상 반환
            return routePoints;
        }

        // 4) 2차: 기본 보행자(ways_walk) 기준으로는 길이 있는지 확인
        List<RoutePointDto> walkRoute = routingRepository.findRoutePoints(
                startVertex,
                endVertex,
                "ways_walk"
        );

        if (walkRoute.isEmpty()) {
            // 어떤 프로필로도 길이 없음 → 진짜로 경로 X
            throw new CustomException(MapErrorCode.ROUTE_NOT_FOUND);
        }

        // 보행자는 가능하지만, 현재 MobilityType에는 부적합
        // MapErrorCode.ROUTENOTSUITABLEMOBILIYT 메시지에 %s 가 있으니,
        // CustomException 쪽에서 포맷 지원 못하면 메시지 직접 포맷해서 새 에러코드 만들거나
        // 에러코드 메시지에서 %s를 빼는 것도 고려.
        throw new CustomException(MapErrorCode.ROUTE_NOT_SUITABLE_MOBILITY);
    }

    private String resolveWaysViewName(MobilityProfile profile) {
        if (profile.isAvoidStairs() && profile.isAvoidHighSlope()) {
            // 계단 X, 고경사 X → 휠체어나 노인
            return "ways_wheelchair";
        }
        if (profile.isAvoidStairs() && !profile.isAvoidHighSlope()) {
            // 계단 X, 경사 OK → 유모차
            return "ways_stroller";
        }
        return "ways_walk";
    }

}
