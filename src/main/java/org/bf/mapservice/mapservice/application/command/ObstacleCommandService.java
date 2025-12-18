package org.bf.mapservice.mapservice.application.command;

import org.bf.mapservice.mapservice.application.query.ObstacleDefaults;
import org.bf.mapservice.mapservice.domain.entity.*;
import org.bf.mapservice.mapservice.infrastructure.repository.ObstacleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObstacleCommandService {

    private final ObstacleRepository obstacleRepository;
    private final ObstacleDefaults defaults;

    public ObstacleCommandService(ObstacleRepository obstacleRepository, ObstacleDefaults defaults) {
        this.obstacleRepository = obstacleRepository;
        this.defaults = defaults;
    }

    @Transactional
    public Long create(CreateObstacleCommand cmd) {
        // 1) severity 보정
        Severity severity = cmd.severity();
        if (severity == null) {
            severity = defaults.defaultSeverity(cmd.type());
        }
        if (severity == null) {
            severity = Severity.MEDIUM;
        }

        // 2) radius 보정 (null/0/음수 방어) + 최종 확정 저장
        Integer radius = cmd.radiusMeters();
        if (radius == null || radius <= 0) {
            int def = defaults.defaultRadiusMeters(cmd.type());
            radius = (def > 0) ? def : null;
        }
        if (radius == null || radius <= 0) {
            radius = (cmd.geomType() == ObstacleGeometryType.POINT) ? 15 : 20;
        }

        Obstacle obstacle = new Obstacle(
                cmd.geom(),
                cmd.geomType(),
                cmd.type(),
                severity,
                ObstacleStatus.ACTIVE,
                radius,
                cmd.startsAt(),
                cmd.endsAt(),
                50
        );

        return obstacleRepository.save(obstacle).getId();
    }

    @Transactional
    public void resolve(Long id) {
        Obstacle o = obstacleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OBSTACLE_NOT_FOUND"));
        o.resolve();
    }
}