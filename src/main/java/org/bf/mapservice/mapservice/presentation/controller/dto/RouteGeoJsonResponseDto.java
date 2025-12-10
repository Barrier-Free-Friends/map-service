package org.bf.mapservice.mapservice.presentation.controller.dto;

import java.util.List;

public record RouteGeoJsonResponseDto(
        String type,
        List<List<Double>> coordinates
) {
    public RouteGeoJsonResponseDto(List<RoutePointDto> points){
        this("LineString", points.stream()
                .map(p -> List.of(p.longitude(),p.latitude()))
                .toList());
    }
}
