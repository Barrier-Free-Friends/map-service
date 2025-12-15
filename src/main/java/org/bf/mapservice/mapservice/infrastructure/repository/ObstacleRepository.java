package org.bf.mapservice.mapservice.infrastructure.repository;

import org.bf.mapservice.mapservice.domain.entity.Obstacle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObstacleRepository extends JpaRepository<Obstacle, Long> {
}
