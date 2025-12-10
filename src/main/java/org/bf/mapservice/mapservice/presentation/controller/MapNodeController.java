package org.bf.mapservice.mapservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.bf.global.infrastructure.CustomResponse;
import org.bf.mapservice.mapservice.application.service.MapNodeQueryService;
import org.bf.mapservice.mapservice.presentation.controller.dto.NearestNodeResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/map/nodes")
@RequiredArgsConstructor
public class MapNodeController {

    private final MapNodeQueryService mapNodeQueryService;

    @GetMapping("/nearest")
    public CustomResponse<NearestNodeResponseDto> findNearest(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        NearestNodeResponseDto dto = mapNodeQueryService.findNearestNode(lat, lng);
        return CustomResponse.onSuccess(dto);
    }
}