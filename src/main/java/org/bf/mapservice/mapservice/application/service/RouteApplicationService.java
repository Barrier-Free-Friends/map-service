package org.bf.mapservice.mapservice.application.service;

import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.domain.entity.MapEdge;
import org.bf.mapservice.mapservice.domain.entity.MapNode;
import org.bf.mapservice.mapservice.domain.entity.MobilityProfile;
import org.bf.mapservice.mapservice.domain.repository.MapEdgeRepository;
import org.bf.mapservice.mapservice.domain.repository.MapNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class RouteApplicationService {

    private final MapNodeRepository mapNodeRepository;
    private final MapEdgeRepository mapEdgeRepository;

    public RouteApplicationService(
            MapNodeRepository mapNodeRepository,
            MapEdgeRepository mapEdgeRepository
    ) {
        this.mapNodeRepository = mapNodeRepository;
        this.mapEdgeRepository = mapEdgeRepository;
    }

    public List<Long> findRoute(FindRouteQuery query) {
        MobilityProfile profile = MobilityProfile.from(query.mobilityType());

        MapNode startNode = mapNodeRepository.findNearestNode(
                        query.startLatitude(), query.startLongitude())
                .orElseThrow(() -> new IllegalArgumentException("시작 지점 근처 노드를 찾을 수 없습니다."));

        MapNode endNode = mapNodeRepository.findNearestNode(
                        query.endLatitude(), query.endLongitude())
                .orElseThrow(() -> new IllegalArgumentException("도착 지점 근처 노드를 찾을 수 없습니다."));

        // 간단한 bounding box 예시 (경도/위도 ±0.01 정도)
        double minLat = Math.min(query.startLatitude(), query.endLatitude()) - 0.01;
        double maxLat = Math.max(query.startLatitude(), query.endLatitude()) + 0.01;
        double minLng = Math.min(query.startLongitude(), query.endLongitude()) - 0.01;
        double maxLng = Math.max(query.startLongitude(), query.endLongitude()) + 0.01;

        List<MapEdge> edges = mapEdgeRepository.findEdgesWithinBoundingBox(
                minLat, minLng, maxLat, maxLng
        );

        // 메모리상 그래프 구성
        Map<Long, List<MapEdge>> adjacency = buildAdjacency(edges);

        return dijkstra(startNode.getId(), endNode.getId(), adjacency, profile);
    }

    private Map<Long, List<MapEdge>> buildAdjacency(List<MapEdge> edges) {
        Map<Long, List<MapEdge>> adjacency = new HashMap<>();
        for (MapEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>())
                    .add(edge);
        }
        return adjacency;
    }

    private List<Long> dijkstra(
            Long startNodeId,
            Long endNodeId,
            Map<Long, List<MapEdge>> adjacency,
            MobilityProfile profile
    ) {
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<NodeCost> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeCost::cost));

        dist.put(startNodeId, 0.0);
        pq.offer(new NodeCost(startNodeId, 0.0));

        while (!pq.isEmpty()) {
            NodeCost current = pq.poll();
            Long currentNodeId = current.nodeId();

            // 이미 더 짧은 거리로 방문한 적이 있으면 스킵
            if (current.cost() > dist.getOrDefault(currentNodeId, Double.POSITIVE_INFINITY)) {
                continue;
            }

            // 목적지에 도달했으면 조기 종료 (prev는 이미 세팅된 상태)
            if (Objects.equals(currentNodeId, endNodeId)) {
                break;
            }

            for (MapEdge edge : adjacency.getOrDefault(currentNodeId, List.of())) {
                if (!edge.isPassableFor(profile)) {
                    continue;
                }

                double weight = edge.calculateWeight(profile);
                double nextCost = current.cost() + weight;
                Long nextNodeId = edge.getTargetNodeId();

                if (nextCost < dist.getOrDefault(nextNodeId, Double.POSITIVE_INFINITY)) {
                    dist.put(nextNodeId, nextCost);
                    prev.put(nextNodeId, currentNodeId);    // ← 이 부분이 핵심
                    pq.offer(new NodeCost(nextNodeId, nextCost));
                }
            }
        }

        return reconstructPath(startNodeId, endNodeId, prev);
    }

    private List<Long> reconstructPath(
            Long startNodeId,
            Long endNodeId,
            Map<Long, Long> prev
    ) {
        // start != end인데 end로 가는 이전 노드 정보가 없다 → 경로 없음
        if (!Objects.equals(startNodeId, endNodeId) && !prev.containsKey(endNodeId)) {
            return List.of();
        }

        List<Long> path = new ArrayList<>();
        Long current = endNodeId;

        // end에서 start까지 거슬러 올라가기
        while (current != null) {
            path.add(current);
            if (Objects.equals(current, startNodeId)) {
                break;
            }
            current = prev.get(current);

            // 방어 코드: 중간에 끊기면 경로 없음으로 처리
            if (current == null) {
                return List.of();
            }
        }

        Collections.reverse(path);
        return path;
    }

    private record NodeCost(Long nodeId, double cost) {}
}
