package org.bf.mapservice.mapservice.presentation.controller.dto;

import java.util.List;

public record RouteResponseDto(
        List<RoutePointDto> points
) {}