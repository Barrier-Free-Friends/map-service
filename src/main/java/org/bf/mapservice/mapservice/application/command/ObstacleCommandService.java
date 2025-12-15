package org.bf.mapservice.mapservice.application.command;

import org.bf.mapservice.mapservice.domain.entity.Obstacle;
import org.bf.mapservice.mapservice.domain.entity.ObstacleStatus;
import org.bf.mapservice.mapservice.infrastructure.repository.ObstacleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObstacleCommandService {

    private final ObstacleRepository obstacleRepository;

    public ObstacleCommandService(ObstacleRepository obstacleRepository) {
        this.obstacleRepository = obstacleRepository;
    }

    @Transactional
    public Long create(CreateObstacleCommand cmd) {
        // 기본값: ACTIVE, confidence=50 (MVP)
        Obstacle obstacle = new Obstacle(
                cmd.geom(),
                cmd.geomType(),
                cmd.type(),
                cmd.severity(),
                ObstacleStatus.ACTIVE,
                cmd.radiusMeters(),
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