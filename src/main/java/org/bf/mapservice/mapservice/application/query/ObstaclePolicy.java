package org.bf.mapservice.mapservice.application.query;

import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.bf.mapservice.mapservice.domain.entity.Severity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObstaclePolicy {

    private final ObstacleDefaults defaults;

    public Decision decide(MobilityType mobility, ObstacleType type, Severity severity) {

        Severity s = resolveSeverity(type, severity);

        // 1) 계단
        if (type == ObstacleType.STAIRS) {
            return mobility.isBarrierSensitive()
                    ? Decision.ofBlock()
                    : Decision.penalty(0.3);
        }

        // 2) 엘리베이터 고장
        if (type == ObstacleType.ELEVATOR_OUTAGE) {
            if (mobility.isBarrierSensitive()) {
                return (s == Severity.CRITICAL || s == Severity.HIGH)
                        ? Decision.ofBlock()
                        : Decision.penalty(0.2);
            }
            return Decision.ignore();
        }

        // 3) 도로/보도 막힘: CRITICAL이면 차단 유지(진짜 막힘)
        if (type == ObstacleType.SIDEWALK_BLOCKED || type == ObstacleType.ROAD_BLOCKED) {
            return switch (s) {
                case CRITICAL -> Decision.ofBlock();
                case HIGH -> Decision.penalty(0.1);
                case MEDIUM -> Decision.penalty(0.4);
                case LOW -> Decision.penalty(0.7);
            };
        }

        // ------------------------------------------------------------
        // ✅ B안 확장: 아래 타입들은 CRITICAL이어도 "무조건 0.0"로 떨어지지 않게
        // policy에서 severity->penalty를 직접 매핑한다.
        // (defaults.applySeverityToMultiplyBy는 CRITICAL->0.0이므로 타지 않게 함)
        // ------------------------------------------------------------

        if (type == ObstacleType.CONSTRUCTION) {
            return bySeverity(s, 0.2, 0.3, 0.5, 0.7);
        }

        if (type == ObstacleType.TREE || type == ObstacleType.ROCK) {
            // base=0.3인 타입들: CRITICAL도 차단 대신 강한 우회
            return bySeverity(s, 0.2, 0.3, 0.5, 0.7);
        }

        if (type == ObstacleType.FURNITURE) {
            // base=0.5: 너무 세게 막지 않도록 한 단계 완화
            return bySeverity(s, 0.35, 0.5, 0.7, 0.85);
        }

        if (type == ObstacleType.SLOPE) {
            // slope는 휠체어에서 base가 0.0(차단)인 정책이 이미 defaults에 있음 → 유지
            double base = defaults.baseMultiplyBy(type, mobility);
            if (base <= 0.0) return Decision.ofBlock();

            // base가 0이 아닌 경우(보행자/유모차/노약자 등)는 CRITICAL도 강한 우회
            return bySeverity(s, 0.25, 0.4, 0.7, 0.85);
        }

        if (type == ObstacleType.OTHER_OBSTACLE) {
            // base=0.6: 영향 약한 편이라 CRITICAL도 0.0까지는 안 내림
            return bySeverity(s, 0.4, 0.55, 0.75, 0.9);
        }

        // 4) 그 외 타입: 기존 Defaults 로직 사용
        double base = defaults.baseMultiplyBy(type, mobility);
        double adjusted = defaults.applySeverityToMultiplyBy(base, s);

        if (adjusted <= 0.0) return Decision.ofBlock();
        if (adjusted >= 1.0) return Decision.ignore();
        return Decision.penalty(adjusted);
    }

    private Decision bySeverity(Severity s, double critical, double high, double medium, double low) {
        double mul = switch (s) {
            case CRITICAL -> critical;
            case HIGH -> high;
            case MEDIUM -> medium;
            case LOW -> low;
        };
        if (mul <= 0.0) return Decision.ofBlock();
        if (mul >= 1.0) return Decision.ignore();
        return Decision.penalty(mul);
    }

    private Severity resolveSeverity(ObstacleType type, Severity severity) {
        if (severity != null) return severity;

        Severity byType = defaults.defaultSeverity(type);
        return (byType != null) ? byType : Severity.MEDIUM;
    }
}