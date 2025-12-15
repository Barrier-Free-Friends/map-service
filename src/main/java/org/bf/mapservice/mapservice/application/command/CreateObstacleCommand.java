package org.bf.mapservice.mapservice.application.command;

import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.bf.mapservice.mapservice.domain.entity.Severity;
import org.locationtech.jts.geom.Geometry;

import java.time.OffsetDateTime;

public record CreateObstacleCommand(
        Geometry geom,
        ObstacleGeometryType geomType,
        ObstacleType type,
        Severity severity,
        Integer radiusMeters,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {}