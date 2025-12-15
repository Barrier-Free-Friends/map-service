package org.bf.mapservice.mapservice.infrastructure.persistence;

import org.bf.mapservice.mapservice.domain.entity.Obstacle;

import java.time.OffsetDateTime;
import java.util.List;

public interface ObstacleQueryDao {
    List<Obstacle> findActiveObstaclesInEnvelope(
            double minLon, double minLat, double maxLon, double maxLat,
            OffsetDateTime now
    );
}