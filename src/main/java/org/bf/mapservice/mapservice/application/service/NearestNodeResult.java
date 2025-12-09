package org.bf.mapservice.mapservice.application.service;

public record NearestNodeResult(
        Long nodeId,
        double distanceMeters
) { }

