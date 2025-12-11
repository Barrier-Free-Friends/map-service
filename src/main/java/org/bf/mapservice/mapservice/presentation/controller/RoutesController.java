package org.bf.mapservice.mapservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.bf.global.infrastructure.CustomResponse;
import org.bf.mapservice.mapservice.application.service.dto.FindRouteQuery;
import org.bf.mapservice.mapservice.application.service.query.RouteApplicationService;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteGeoJsonResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/routes")
public class RoutesController {

    private final RouteApplicationService routeApplicationService;

    @PostMapping
    public CustomResponse<RouteGeoJsonResponseDto> findRoute(@RequestBody RouteRequestDto request) {
        FindRouteQuery query = new FindRouteQuery(
                request.startLatitude(),
                request.startLongitude(),
                request.endLatitude(),
                request.endLongitude(),
                request.mobilityType()
        );
        // 1) 서비스에서 (lat, lon) 리스트 받아오고
        List<RoutePointDto> points = routeApplicationService.findRoute(query);
        // 2) GeoJSON LineString으로 감싸서 응답
        RouteGeoJsonResponseDto response = new RouteGeoJsonResponseDto(points);
        return CustomResponse.onSuccess(response);
    }

    @PostMapping("/detail")
    public CustomResponse<RouteDetailResponseDto> findRouteDetail(@RequestBody RouteRequestDto request) {
        FindRouteQuery query = new FindRouteQuery(
                request.startLatitude(),
                request.startLongitude(),
                request.endLatitude(),
                request.endLongitude(),
                request.mobilityType()
        );
        RouteDetailResponseDto response = routeApplicationService.findRouteDetail(query);
        return CustomResponse.onSuccess(response);
    }
}
