package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.bf.mapservice.mapservice.domain.entity.Severity;
import org.springframework.stereotype.Component;

@Component
public class ObstaclePolicy {

    public Decision decide(MobilityType mobility, ObstacleType type, Severity severity) {

        // 1) 계단: 휠체어/유모차/노약자 차단
        if (type == ObstacleType.STAIRS) {
            if (mobility == MobilityType.WHEELCHAIR || mobility == MobilityType.STROLLER || mobility == MobilityType.ELDERLY) {
                return Decision.ofBlock();
            }
            // 보행자는 큰 패널티 정도로만
            return Decision.penalty(0.3);
        }

        // 2) 공사/보도막힘/도로막힘: 심각도 기반
        if (type == ObstacleType.CONSTRUCTION || type == ObstacleType.SIDEWALK_BLOCKED || type == ObstacleType.ROAD_BLOCKED) {
            return switch (severity) {
                case CRITICAL -> Decision.ofBlock();
                case HIGH -> Decision.penalty(0.1);
                case MEDIUM -> Decision.penalty(0.4);
                case LOW -> Decision.penalty(0.7);
            };
        }

        // 3) 엘리베이터 고장: 휠체어/노약자만 강하게
        if (type == ObstacleType.ELEVATOR_OUTAGE) {
            if (mobility == MobilityType.WHEELCHAIR || mobility == MobilityType.ELDERLY) {
                return (severity == Severity.CRITICAL || severity == Severity.HIGH) ? Decision.ofBlock() : Decision.penalty(0.2);
            }
            return Decision.ignore();
        }

        // default
        return Decision.penalty(0.8);
    }

}