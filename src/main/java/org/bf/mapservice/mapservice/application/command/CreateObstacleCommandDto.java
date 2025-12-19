package org.bf.mapservice.mapservice.application.command;

import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.bf.mapservice.mapservice.domain.entity.Severity;
import org.locationtech.jts.geom.Geometry;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateObstacleCommandDto(
        Geometry geom,
        ObstacleGeometryType geomType,
        ObstacleType type,
        Severity severity,
        Integer radiusMeters,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        UUID userId
) {}

