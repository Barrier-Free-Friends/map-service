package org.bf.mapservice.mapservice.application.dto;

public record NearestNodeResult(
        Long nodeId,
        double distanceMeters
) { }

