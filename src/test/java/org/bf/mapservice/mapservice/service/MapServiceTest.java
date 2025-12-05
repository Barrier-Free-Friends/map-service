package org.bf.mapservice.mapservice.service;

import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.application.service.RouteApplicationService;
import org.bf.mapservice.mapservice.domain.entity.MapEdge;
import org.bf.mapservice.mapservice.domain.entity.MapNode;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.repository.MapEdgeRepository;
import org.bf.mapservice.mapservice.domain.repository.MapNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class MapServiceTest {

    @Autowired
    private RouteApplicationService routeApplicationService;

    @MockitoBean
    private MapNodeRepository mapNodeRepository;

    @MockitoBean
    private MapEdgeRepository mapEdgeRepository;

    private MapNode startNode;
    private MapNode midNode;
    private MapNode endNode;

    @BeforeEach
    void setUp() {
        // 테스트용 노드 3개 생성 (id는 Reflection으로 주입)
        startNode = MapNode.builder()
                .latitude(35.0)
                .longitude(129.0)
                .build();
        midNode = MapNode.builder()
                .latitude(35.0005)
                .longitude(129.0005)
                .build();
        endNode = MapNode.builder()
                .latitude(35.001)
                .longitude(129.001)
                .build();

        ReflectionTestUtils.setField(startNode, "id", 1L);
        ReflectionTestUtils.setField(midNode, "id", 2L);
        ReflectionTestUtils.setField(endNode, "id", 3L);
    }

    private MapEdge edge(
            Long sourceId,
            Long targetId,
            double distanceM,
            double slope,
            boolean hasStairs,
            boolean hasSidewalk
    ) {
        return MapEdge.builder()
                .osmWayId(null)
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .distanceM(distanceM)
                .slope(slope)
                .hasStairs(hasStairs)
                .hasRamp(!hasStairs) // 단순 가정
                .hasSidewalk(hasSidewalk)
                .build();
    }

    @Test
    @Transactional
    @DisplayName("경로 탐색: 가장 단순한 그래프에서 기본 최단경로를 반환한다")
    void findRoute_basicShortestPath() {
        // given
        // start(1) -> end(3): 100m
        MapEdge direct = edge(1L, 3L, 100.0, 0.0, false, true);

        // start(1) -> mid(2) -> end(3): 60m + 60m = 120m (더 길다)
        MapEdge via1 = edge(1L, 2L, 60.0, 0.0, false, true);
        MapEdge via2 = edge(2L, 3L, 60.0, 0.0, false, true);

        when(mapNodeRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(startNode), Optional.of(endNode));

        when(mapEdgeRepository.findEdgesWithinBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of(direct, via1, via2));

        FindRouteQuery query = new FindRouteQuery(
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude(),
                MobilityType.WHEELCHAIR // 어떤 타입이든 상관 없음
        );

        // when
        List<Long> path = routeApplicationService.findRoute(query);

        // then
        assertThat(path).containsExactly(1L, 3L); // 직접 경로가 선택되어야 함
    }

    @Test
    @Transactional
    @DisplayName("경로 탐색: MobilityProfile(휠체어)은 계단이 있는 엣지를 통과하지 않는다")
    void findRoute_wheelchair_avoidsStairs() {
        // given
        // 1 -> 3: 50m 이지만 계단 있음
        MapEdge stairsEdge = edge(1L, 3L, 50.0, 0.0, true, true);

        // 1 -> 2 -> 3: 총 120m, 계단 없음
        MapEdge noStairs1 = edge(1L, 2L, 60.0, 0.0, false, true);
        MapEdge noStairs2 = edge(2L, 3L, 60.0, 0.0, false, true);

        when(mapNodeRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(startNode), Optional.of(endNode));

        when(mapEdgeRepository.findEdgesWithinBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of(stairsEdge, noStairs1, noStairs2));

        FindRouteQuery query = new FindRouteQuery(
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude(),
                MobilityType.WHEELCHAIR
        );

        // when
        List<Long> path = routeApplicationService.findRoute(query);

        // then
        // 계단 있는 1->3은 Hard constraint로 인해 제거되고
        // 1 -> 2 -> 3 경로가 선택되어야 함
        assertThat(path).containsExactly(1L, 2L, 3L);
    }

    @Test
    @Transactional
    @DisplayName("경로 탐색: 휠체어는 경사 높은 경로를 피하고 완만한 경로를 선택한다")
    void findRoute_wheelchair_avoids_high_slope() {
        MapEdge gentle = edge(1L, 3L, 100.0, 0.0, false, true);
        MapEdge steep1 = edge(1L, 2L, 30.0, 9.0, false, true);
        MapEdge steep2 = edge(2L, 3L, 30.0, 9.0, false, true);

        when(mapNodeRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(startNode), Optional.of(endNode));

        when(mapEdgeRepository.findEdgesWithinBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of(gentle, steep1, steep2));

        FindRouteQuery query = new FindRouteQuery(
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude(),
                MobilityType.WHEELCHAIR
        );

        List<Long> path = routeApplicationService.findRoute(query);

        assertThat(path).containsExactly(1L, 3L);
    }

    @Test
    @Transactional
    @DisplayName("경로 탐색: 유모차는 경사에 덜 민감해 더 짧은 경로를 선택한다")
    void findRoute_stroller_prefers_shorter_even_if_steep() {
        MapEdge gentle = edge(1L, 3L, 100.0, 0.0, false, true);
        MapEdge steep1 = edge(1L, 2L, 30.0, 9.0, false, true);
        MapEdge steep2 = edge(2L, 3L, 30.0, 9.0, false, true);

        when(mapNodeRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(startNode), Optional.of(endNode));

        when(mapEdgeRepository.findEdgesWithinBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of(gentle, steep1, steep2));

        FindRouteQuery query = new FindRouteQuery(
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude(),
                MobilityType.STROLLER
        );

        List<Long> path = routeApplicationService.findRoute(query);

        assertThat(path).containsExactly(1L, 2L, 3L);
    }


    @Test
    @Transactional
    @DisplayName("경로 탐색: 서브그래프에 엣지가 없으면 빈 경로를 반환한다")
    void findRoute_noEdges_returnsEmptyPath() {
        // given
        when(mapNodeRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(startNode), Optional.of(endNode));

        when(mapEdgeRepository.findEdgesWithinBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of());

        FindRouteQuery query = new FindRouteQuery(
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude(),
                MobilityType.WHEELCHAIR
        );

        // when
        List<Long> path = routeApplicationService.findRoute(query);

        // then
        assertThat(path).isEmpty();
    }
}
