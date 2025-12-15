package org.bf.mapservice.mapservice.presentation.controller.dto;

public record RouteEdgeDto(
        int seq,
        long edgeId,
        String highway,
        String surface,
        double lengthMeters,
        boolean stairs,
        boolean passable,
        String notPassableReason
) {}