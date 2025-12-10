package org.bf.mapservice.mapservice.controller;

import org.bf.global.infrastructure.exception.CustomException;
import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.application.service.RouteApplicationService;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.exception.MapErrorCode;
import org.bf.mapservice.mapservice.infrastructure.persistence.RoutingRepository;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteApplicationServiceTest {

    @Mock
    RoutingRepository routingRepository;

    RouteApplicationService routeApplicationService;

    @BeforeEach
    void setUp() {
        routeApplicationService = new RouteApplicationService(routingRepository);
    }

    private FindRouteQuery makeQuery(MobilityType mobilityType) {
        return new FindRouteQuery(
                37.489,   // startLat
                127.034,  // startLng
                37.491,   // endLat
                127.037,  // endLng
                mobilityType
        );
    }

    @Test
    void 프로필뷰에서_경로를_찾으면_그대로_반환한다() {
        // given
        FindRouteQuery query = makeQuery(MobilityType.PEDESTRIAN);

        long fakeStartVertex = 1L;
        long fakeEndVertex   = 2L;

        given(routingRepository.findNearestVertex(query.startLatitude(), query.startLongitude()))
                .willReturn(fakeStartVertex);
        given(routingRepository.findNearestVertex(query.endLatitude(), query.endLongitude()))
                .willReturn(fakeEndVertex);

        // 프로필 뷰(이 케이스는 PEDESTRIAN → ways_walk)에서 바로 경로가 나온다고 가정
        List<RoutePointDto> profileRoute = List.of(
                new RoutePointDto(37.4890, 127.0340),
                new RoutePointDto(37.4900, 127.0350)
        );

        given(routingRepository.findRoutePoints(fakeStartVertex, fakeEndVertex, "ways_walk"))
                .willReturn(profileRoute);

        // when
        List<RoutePointDto> result = routeApplicationService.findRoute(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).latitude()).isEqualTo(37.4890);
        verify(routingRepository).findRoutePoints(fakeStartVertex, fakeEndVertex, "ways_walk");
    }

    @Test
    void 어떤_프로필로도_경로를_찾지_못하면_ROUTE_NOT_FOUND를_던진다() {
        // given
        FindRouteQuery query = makeQuery(MobilityType.WHEELCHAIR);

        long fakeStartVertex = 1L;
        long fakeEndVertex   = 2L;

        given(routingRepository.findNearestVertex(query.startLatitude(), query.startLongitude()))
                .willReturn(fakeStartVertex);
        given(routingRepository.findNearestVertex(query.endLatitude(), query.endLongitude()))
                .willReturn(fakeEndVertex);

        // 1차: 휠체어 뷰(ways_wheelchair)에서는 경로 없음
        given(routingRepository.findRoutePoints(fakeStartVertex, fakeEndVertex, "ways_wheelchair"))
                .willReturn(List.of());

        // 2차: 기본 보행자 뷰(ways_walk)에서도 경로 없음
        given(routingRepository.findRoutePoints(fakeStartVertex, fakeEndVertex, "ways_walk"))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> routeApplicationService.findRoute(query))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getCode()).isEqualTo(MapErrorCode.ROUTE_NOT_FOUND);
                });
    }

    @Test
    void 보행자는_갈수있지만_현재_MobilityType으로는_갈수없으면_ROUTE_NOTSUITABLE을_던진다() {
        // given
        FindRouteQuery query = makeQuery(MobilityType.WHEELCHAIR);

        long fakeStartVertex = 1L;
        long fakeEndVertex   = 2L;

        given(routingRepository.findNearestVertex(query.startLatitude(), query.startLongitude()))
                .willReturn(fakeStartVertex);
        given(routingRepository.findNearestVertex(query.endLatitude(), query.endLongitude()))
                .willReturn(fakeEndVertex);

        // 1차: 휠체어용 뷰에서는 경로 없음
        given(routingRepository.findRoutePoints(fakeStartVertex, fakeEndVertex, "ways_wheelchair"))
                .willReturn(List.of());

        // 2차: 보행용 뷰에서는 경로 있음
        List<RoutePointDto> walkRoute = List.of(
                new RoutePointDto(37.4890, 127.0340),
                new RoutePointDto(37.4900, 127.0350)
        );
        given(routingRepository.findRoutePoints(fakeStartVertex, fakeEndVertex, "ways_walk"))
                .willReturn(walkRoute);

        // when & then
        assertThatThrownBy(() -> routeApplicationService.findRoute(query))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getCode()).isEqualTo(MapErrorCode.ROUTE_NOT_SUITABLE_MOBILITY);
                });
    }
}
