package org.bf.mapservice.mapservice.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.application.service.RouteApplicationService;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/routes")
public class RoutesController {

    private final RouteApplicationService routeApplicationService;

    @PostMapping
    public ResponseEntity<RouteResponseDto> findRoute(@RequestBody RouteRequestDto request) {
        FindRouteQuery query = new FindRouteQuery(
                request.startLatitude(),
                request.startLongitude(),
                request.endLatitude(),
                request.endLongitude(),
                request.mobilityType()
        );

        List<RoutePointDto> points = routeApplicationService.findRoute(query);
        return ResponseEntity.ok(new RouteResponseDto(points));
    }
}
