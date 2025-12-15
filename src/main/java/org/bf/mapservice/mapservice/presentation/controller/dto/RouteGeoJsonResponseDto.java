package org.bf.mapservice.mapservice.presentation.controller.dto;

import java.util.List;

public record RouteGeoJsonResponseDto(
        String type,
        List<List<Double>> coordinates
) {}