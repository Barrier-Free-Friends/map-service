package org.bf.mapservice.mapservice.presentation.controller.dto;

import java.util.List;

public record RouteDetailResponseDto(
        double totalDistanceMeters,
        RouteGeoJsonResponseDto route,
        List<RouteEdgeDto> edges,
        boolean fullyAccessible,
        Integer accessibleUntilSeq,
        String firstBlockedReason,
        String requestedMobilityType
) {}