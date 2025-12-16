package org.bf.mapservice.mapservice.presentation.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.bf.mapservice.mapservice.domain.entity.Severity;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateObstacleRequestDto(
        @NotNull ObstacleGeometryType geomType,   // POINT | LINESTRING
        @NotNull ObstacleType type,
        @NotNull Severity severity,

        // POINT: [lon,lat]
        List<Double> point,

        // LINESTRING: [[lon,lat], [lon,lat], ...]
        List<List<Double>> line,

        Integer radiusMeters,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {}