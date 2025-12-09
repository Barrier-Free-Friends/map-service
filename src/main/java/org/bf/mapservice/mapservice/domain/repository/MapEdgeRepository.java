package org.bf.mapservice.mapservice.domain.repository;

import org.bf.mapservice.mapservice.domain.entity.MapEdge;

import java.util.List;

public interface MapEdgeRepository {

    // 시작/종료 근처의 서브그래프 로딩용 메서드들도 추가 가능
    List<MapEdge> findEdgesWithinBoundingBox(
            double minLat, double minLng,
            double maxLat, double maxLng
    );
}
