package org.bf.mapservice.mapservice.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.presentation.controller.dto.NearestNodeResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteEdgeDto;
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
               CAST(:startId AS integer),
               CAST(:endId AS integer)
             ) AS d
        JOIN planet_osm_line_vertices_pgr AS v
          ON d.node = v.id
        WHERE d.edge <> -1
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

    /**
     * (lat, lng) 기준으로 ways_walk 네트워크에 실제 연결된 vertex 중
     * 가장 가까운 vertex + 거리 정보 조회.
     *
     * /v1/map/nodes/nearest 에서 사용.
     * -> 라우팅에 실제 사용 가능한 노드만 대상으로 함.
     */
    public NearestNodeResponseDto findNearestVertexWithDistance(double latitude, double longitude) {
        String sql = """
        SELECT
          v.id,
          ST_Y(ST_Transform(v.the_geom, 4326)) AS lat,
          ST_X(ST_Transform(v.the_geom, 4326)) AS lng,
          ST_Distance(
            v.the_geom,
            ST_Transform(
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
              3857
            )
          ) AS distance_m
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

        Map<String, Object> params = Map.of(
                "lat", latitude,
                "lng", longitude
        );

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                new NearestNodeResponseDto(
                        rs.getLong("id"),
                        rs.getDouble("lat"),
                        rs.getDouble("lng"),
                        rs.getDouble("distance_m"),
                        false,      // TODO: hasElevator
                        false       // TODO: isEntrance
                )
        );
    }

    public List<RouteEdgeDto> findRouteEdges(
            long startVertexId,
            long endVertexId,
            String waysViewName
    ) {
        String sql = """
        SELECT
            d.seq,
            w.id AS edge_id,
            w.highway,
            w.surface,
            ST_Length(w.geom) AS length_m,
            CASE
                WHEN w.highway IN ('steps', 'step') THEN TRUE
                ELSE FALSE
            END AS stairs
        FROM pgr_dijkstra(
               'SELECT id, source, target, cost, reverse_cost FROM %s',
               CAST(:startId AS integer),
               CAST(:endId AS integer)
             ) AS d
        JOIN %s w
          ON d.edge = w.id
        WHERE d.edge <> -1
        ORDER BY d.seq
        """.formatted(waysViewName, waysViewName);

        Map<String, Object> params = Map.of(
                "startId", startVertexId,
                "endId", endVertexId
        );

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new RouteEdgeDto(
                        rs.getInt("seq"),
                        rs.getLong("edge_id"),
                        rs.getString("highway"),
                        rs.getString("surface"),
                        rs.getDouble("length_m"),
                        rs.getBoolean("stairs")
                )
        );
    }

}