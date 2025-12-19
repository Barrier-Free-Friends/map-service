package org.bf.mapservice.mapservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.application.query.RouteApplicationService;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.bf.mapservice.mapservice.presentation.docs.RoutesApiDoc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RoutesController implements RoutesApiDoc {

    private final RouteApplicationService routeApplicationService;

    @PostMapping("/detail")
    public RouteDetailResponseDto findRouteDetail(@RequestBody RouteRequestDto request) {
        return routeApplicationService.findRouteDetail(request);
    }
}