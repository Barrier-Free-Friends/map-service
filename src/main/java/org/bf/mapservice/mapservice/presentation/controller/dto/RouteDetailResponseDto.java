package org.bf.mapservice.mapservice.presentation.controller.dto;

import java.util.List;

public record RouteDetailResponseDto(
        double distanceMeters,               // 총 거리(m)
        RouteGeoJsonResponseDto lineString,  // 기존 LineString DTO 재사용
        List<RouteEdgeDto> segments          // 엣지별 특성 정보
) { }