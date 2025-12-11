package org.bf.mapservice.mapservice.application.service.dto;

public record NearestNodeResult(
        Long nodeId,
        double distanceMeters
) { }

