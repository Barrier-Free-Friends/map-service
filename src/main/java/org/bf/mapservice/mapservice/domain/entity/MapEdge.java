package org.bf.mapservice.mapservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_map_edge")
public class MapEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osm_way_id")
    private Long osmWayId;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    @Column(name = "distance_m", nullable = false)
    private double distanceM;

    @Column(nullable = false)
    private double slope;

    @Column(name = "has_stairs", nullable = false)
    private boolean hasStairs;

    @Column(name = "has_ramp", nullable = false)
    private boolean hasRamp;

    @Column(name = "has_sidewalk", nullable = false)
    private boolean hasSidewalk;

    @Builder
    public MapEdge(
            Long osmWayId,
            Long sourceNodeId,
            Long targetNodeId,
            double distanceM,
            double slope,
            boolean hasStairs,
            boolean hasRamp,
            boolean hasSidewalk
    ) {
        this.osmWayId = osmWayId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.distanceM = distanceM;
        this.slope = slope;
        this.hasStairs = hasStairs;
        this.hasRamp = hasRamp;
        this.hasSidewalk = hasSidewalk;
    }

    public boolean isPassableFor(MobilityProfile profile) {
        // 1) 계단 회피
        if (profile.isAvoidStairs() && hasStairs) {
            return false;
        }

        // 2) 향후: 차량 전용 도로, 지하도/육교 등도 여기서 차단

        return true;
    }

    public double calculateWeight(MobilityProfile profile) {
        double weight = distanceM;

        // 경사 패널티 (예시 로직)
        if (profile.isAvoidHighSlope()) {
            if (slope > 8.0) {
                weight *= 3.0; // 급경사면 큰 패널티
            } else if (slope > 4.0) {
                weight *= 1.5; // 중간 정도 경사 패널티
            }
        } else {
            // 경사에 민감하지 않은 프로필: 아주 큰 경사만 조금만 패널티
            if (slope > 10.0) {
                weight *= 1.2;
            }
        }

        // 보도 없음 패널티 (1차 MVP에선 Hard 보다는 Soft에 가깝게)
        if (!hasSidewalk) {
            weight *= 2.0;
        }

        return weight;
    }

}