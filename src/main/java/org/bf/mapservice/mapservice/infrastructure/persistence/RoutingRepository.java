package org.bf.mapservice.mapservice.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RoutingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * (lat, lng) 기준으로 ways_* 네트워크에 실제 연결된 vertex 중
     * 가장 가까운 vertex id 조회.
     */
    public Long findNearestVertex(double latitude, double longitude) {
        String sql = """
            SELECT v.id
            FROM planet_osm_line_vertices_pgr AS v
            WHERE EXISTS (
                SELECT 1
                FROM ways_walk AS w
                WHERE w.source = v.id OR w.target = v.id
            )
            ORDER BY v.the_geom <-> ST_Transform(
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                3857
            )
            LIMIT 1
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lat", latitude)
                .addValue("lng", longitude);

        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    /**
     * pgRouting + vertex 좌표까지 한 번에 조회.
     *
     * - 반환: 경로 순서대로 (lat, lon) 리스트
     */
    public List<RoutePointDto> findRoutePoints(long startVertexId,
                                               long endVertexId,
                                               String waysViewName) {

        String sql = """
            SELECT
                d.seq,
                ST_Y(ST_Transform(v.the_geom, 4326)) AS lat,
                ST_X(ST_Transform(v.the_geom, 4326)) AS lng
            FROM pgr_dijkstra(
                   'SELECT id, source, target, cost, reverse_cost FROM %s',
                   :startId,
                   :endId
                 ) AS d
            JOIN planet_osm_line_vertices_pgr AS v
              ON d.node = v.id
            ORDER BY d.seq
            """.formatted(waysViewName);

        Map<String, Object> params = Map.of(
                "startId", startVertexId,
                "endId", endVertexId
        );

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new RoutePointDto(
                        rs.getDouble("lat"),
                        rs.getDouble("lng")
                )
        );
    }

    public RoutePointDto findVertexPoint(long vertexId) {
        String sql = """
            SELECT
              ST_Y(ST_Transform(the_geom, 4326)) AS lat,
              ST_X(ST_Transform(the_geom, 4326)) AS lng
            FROM planet_osm_line_vertices_pgr
            WHERE id = :id
            """;

        Map<String, Object> params = Map.of("id", vertexId);

        return jdbcTemplate.queryForObject(sql, params,
                (rs, rowNum) -> new RoutePointDto(
                        rs.getDouble("lat"),
                        rs.getDouble("lng")
                ));
    }

}
