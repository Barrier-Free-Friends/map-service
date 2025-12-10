package org.bf.mapservice.mapservice.presentation.controller.dto;

public record NearestNodeResponseDto(
        long nodeId,
        double latitude,
        double longitude,
        double distanceMeter,
        boolean hasElevator,
        boolean isEntrance
) {}