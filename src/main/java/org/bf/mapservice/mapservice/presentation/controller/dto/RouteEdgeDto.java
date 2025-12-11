package org.bf.mapservice.mapservice.presentation.controller.dto;

public record RouteEdgeDto(
        int seq,             // pgr_dijkstra seq
        long edgeId,         // ways_* 의 id (= planet_osm_line.osm_id)
        String highway,      // footway / steps / path ...
        String surface,      // 노면 (paving_stones, asphalt, ...)
        double lengthMeters, // 이 엣지 길이 (m)
        boolean stairs       // 계단 여부
) { }
