package org.bf.mapservice.mapservice.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.bf.mapservice.mapservice.domain.entity.Obstacle;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ObstacleQueryDaoImpl implements ObstacleQueryDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Obstacle> findActiveObstaclesInEnvelope(
            double minLon, double minLat, double maxLon, double maxLat,
            OffsetDateTime now
    ) {
        String sql = """
            SELECT *
            FROM obstacle o
            WHERE o.status = 'ACTIVE'
              AND (o.starts_at IS NULL OR o.starts_at <= :now)
              AND (o.ends_at IS NULL OR o.ends_at >= :now)
              AND ST_Intersects(
                    o.geom,
                    ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
                  )
            """;

        return em.createNativeQuery(sql, Obstacle.class)
                .setParameter("now", now)
                .setParameter("minLon", minLon)
                .setParameter("minLat", minLat)
                .setParameter("maxLon", maxLon)
                .setParameter("maxLat", maxLat)
                .getResultList();
    }
}