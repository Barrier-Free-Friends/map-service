package org.bf.mapservice.mapservice.presentation.controller;

import org.bf.mapservice.mapservice.application.query.RouteApplicationService;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/routes")
public class RoutesController {

    private final RouteApplicationService routeApplicationService;

    public RoutesController(RouteApplicationService routeApplicationService) {
        this.routeApplicationService = routeApplicationService;
    }

    @PostMapping("/detail")
    public RouteDetailResponseDto findRouteDetail(@RequestBody RouteRequestDto request) {
        return routeApplicationService.findRouteDetail(request);
    }
}