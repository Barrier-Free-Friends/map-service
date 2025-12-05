package org.bf.mapservice.mapservice.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.bf.mapservice.mapservice.domain.entity.MapNode;
import org.bf.mapservice.mapservice.domain.repository.MapNodeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MapNodeRepositoryImpl implements MapNodeRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<MapNode> findById(Long id) {
        return Optional.ofNullable(em.find(MapNode.class, id));
    }

    @Override
    public Optional<MapNode> findNearestNode(double latitude, double longitude) {
        String sql = """
                SELECT id, latitude, longitude
                FROM p_map_node
                ORDER BY geometry <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
                LIMIT 1
                """;

        var query = em.createNativeQuery(sql, "MapNodeSummaryMapping")
                .setParameter("lat", latitude)
                .setParameter("lng", longitude);

        MapNode result = (MapNode) query.getResultStream().findFirst().orElse(null);
        return Optional.ofNullable(result);
    }
}
