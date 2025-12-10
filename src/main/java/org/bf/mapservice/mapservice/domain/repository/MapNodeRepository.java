package org.bf.mapservice.mapservice.domain.repository;

import org.bf.mapservice.mapservice.domain.entity.MapNode;

import java.util.Optional;

public interface MapNodeRepository {

    // 가장 가까운 노드 조회 (NativeQuery는 infra에서 구현)
    Optional<MapNode> findNearestNode(double latitude, double longitude);
}
