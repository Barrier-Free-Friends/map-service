package org.bf.mapservice.mapservice.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.bf.mapservice.mapservice.domain.entity.MapEdge;
import org.bf.mapservice.mapservice.domain.repository.MapEdgeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MapEdgeRepositoryImpl implements MapEdgeRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<MapEdge> findEdgesFromSource(Long sourceNodeId) {
        String jpql = """
                SELECT e
                FROM MapEdge e
                WHERE e.sourceNodeId = :sourceId
                """;
        return em.createQuery(jpql, MapEdge.class)
                .setParameter("sourceId", sourceNodeId)
                .getResultList();
    }

    @Override
    public List<MapEdge> findEdgesWithinBoundingBox(
            double minLat, double minLng,
            double maxLat, double maxLng
    ) {
        String sql = """
                SELECT *
                FROM p_map_edge e
                JOIN p_map_node n ON e.source_node_id = n.id
                WHERE
                    n.latitude BETWEEN :minLat AND :maxLat
                    AND n.longitude BETWEEN :minLng AND :maxLng
                """;
        return em.createNativeQuery(sql, MapEdge.class)
                .setParameter("minLat", minLat)
                .setParameter("maxLat", maxLat)
                .setParameter("minLng", minLng)
                .setParameter("maxLng", maxLng)
                .getResultList();
    }
}