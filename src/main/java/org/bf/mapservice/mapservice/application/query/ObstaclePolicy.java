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

        // ---------- (1) 기존 로직 유지 ----------
        // 1) 계단: 휠체어/유모차/노약자 차단
        if (type == ObstacleType.STAIRS) {
            if (mobility == MobilityType.WHEELCHAIR
                    || mobility == MobilityType.STROLLER
                    || mobility == MobilityType.ELDERLY) {
                return Decision.ofBlock();
            }
            // 보행자는 큰 패널티 정도로만
            return Decision.penalty(0.3);
        }

        Severity s = resolveSeverity(type, severity);

        // 2) 공사/보도막힘/도로막힘: 심각도 기반 (기존 규칙 유지)
        if (type == ObstacleType.CONSTRUCTION
                || type == ObstacleType.SIDEWALK_BLOCKED
                || type == ObstacleType.ROAD_BLOCKED) {
            return switch (s) {
                case CRITICAL -> Decision.ofBlock();
                case HIGH -> Decision.penalty(0.1);
                case MEDIUM -> Decision.penalty(0.4);
                case LOW -> Decision.penalty(0.7);
            };
        }

        // 3) 엘리베이터 고장: 휠체어/노약자만 강하게 (기존 규칙 유지)
        if (type == ObstacleType.ELEVATOR_OUTAGE) {
            if (mobility == MobilityType.WHEELCHAIR || mobility == MobilityType.ELDERLY) {
                return (s == Severity.CRITICAL || s == Severity.HIGH)
                        ? Decision.ofBlock()
                        : Decision.penalty(0.2);
            }
            return Decision.ignore();
        }

        // ---------- (2) 신규/기타 타입: Defaults로 위임 ----------
        // baseMultiplyBy: 타입/이동수단 기반 기본 가중치
        double base = defaults.baseMultiplyBy(type, mobility);

        // severity로 강화/완화
        double adjusted = defaults.applySeverityToMultiplyBy(base, s);

        // 최종 Decision
        if (adjusted <= 0.0) return Decision.ofBlock();

        // 1.0이면 사실상 영향이 없으니 ignore 처리(원하면 penalty(1.0)로 유지해도 됨)
        if (adjusted >= 1.0) return Decision.ignore();

        return Decision.penalty(adjusted);
    }

    private Severity resolveSeverity(ObstacleType type, Severity severity) {
        if (severity != null) return severity;

        Severity byType = defaults.defaultSeverity(type);
        // defaults가 null을 의도적으로 반환하는 타입들이 있으니 최종 안전값
        return (byType != null) ? byType : Severity.MEDIUM;
    }
}
