package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ObstacleDefaults {

    /**
     * null 의미:
     * - "기본 severity를 강제하지 않겠다"
     * - (예: STAIRS, SIDEWALK_BLOCKED, ROAD_BLOCKED, ELEVATOR_OUTAGE 등은 입력값 또는 별도 규칙이 우선)
     */
    public Severity defaultSeverity(ObstacleType type) {
        return switch (type) {
            case STAIRS -> null;
            case CONSTRUCTION -> Severity.CRITICAL;

            // 신규 타입들(배리어프리 관점 기본값)
            case TREE, ROCK -> Severity.HIGH;
            case FURNITURE -> Severity.MEDIUM;
            case SLOPE -> Severity.HIGH;
            case OTHER_OBSTACLE -> Severity.MEDIUM;

            case SIDEWALK_BLOCKED -> null;
            case ROAD_BLOCKED -> null;
            case ELEVATOR_OUTAGE -> null;
        };
    }

    /**
     * radius 기본값(미터)
     * - 0은 "기본 없음/미사용" 의미로 쓰는 중이지만,
     *   생성 시에는 0이 그대로 저장되지 않도록 ObstacleCommandService에서 보정 권장.
     */
    public int defaultRadiusMeters(ObstacleType type) {
        return switch (type) {
            case STAIRS -> 0;
            case CONSTRUCTION -> 30;

            case TREE, ROCK -> 10;
            case FURNITURE -> 10;
            case SLOPE -> 10;
            case OTHER_OBSTACLE -> 10;

            case SIDEWALK_BLOCKED -> 0;
            case ROAD_BLOCKED -> 0;
            case ELEVATOR_OUTAGE -> 0;
        };
    }

    /* =========================
     *  radius 단일 결정 로직
     * ========================= */

    public int resolveRadiusMeters(Obstacle o) {
        // 1) DB에 명시된 값이 있으면 최우선
        if (o.getRadiusMeters() != null && o.getRadiusMeters() > 0) {
            return o.getRadiusMeters();
        }

        // 2) 타입 기반 기본값
        int byType = defaultRadiusMeters(o.getType());
        if (byType > 0) {
            return byType;
        }

        // 3) 최종 fallback (여기만!)
        return (o.getGeomType() == ObstacleGeometryType.POINT)
                ? 15   // POINT 최소 표시 반경
                : 20;  // LINE / POLYGON
    }

    /**
     * 배리어프리 관점 "타입 기본 가중치"
     * - 0.0 : 차단 (지나갈 수 없음)
     * - 0~1 : 패널티(우회 유도). 작을수록 더 강하게 회피.
     * - 1.0 : 영향 거의 없음(혹은 ignore에 가까움)
     *
     * 주의: STAIRS/ELEVATOR_OUTAGE/SIDEWALK_BLOCKED/ROAD_BLOCKED 등은
     * 기존 ObstaclePolicy에서 별도 규칙이 있으니 기본값만 참고용으로 두거나 NaN 처리해도 됨.
     */
    public double baseMultiplyBy(ObstacleType type, MobilityType mobility) {
        boolean barrierFree =
                mobility == MobilityType.WHEELCHAIR
                        || mobility == MobilityType.STROLLER
                        || mobility == MobilityType.ELDERLY;

        return switch (type) {
            // 기존 정책이 더 강하므로 여기서는 "참고값"
            case STAIRS -> barrierFree ? 0.0 : 0.3;
            case ELEVATOR_OUTAGE -> barrierFree ? 0.0 : 1.0; // 일반 보행자는 ignore에 가까움

            // 신규/일반 장애물
            case CONSTRUCTION -> 0.3;           // 공사는 기본 차단(우회 유도 수준을 넘어선다고 가정)
            case TREE, ROCK -> 0.3;             // 우회 유도 강
            case FURNITURE -> 0.5;              // 우회 유도 중
            case OTHER_OBSTACLE -> 0.6;         // 약한 우회 유도

            // 경사: 배리어프리에서는 치명적일 수 있어서 더 엄격
            case SLOPE -> (mobility == MobilityType.WHEELCHAIR) ? 0.0 : 0.7;

            // 도로/보도 막힘은 severity 기반이 자연스러움(기존 로직 우선)
            case SIDEWALK_BLOCKED, ROAD_BLOCKED -> 0.0; // “막힘” 자체는 강함. 세부는 policy에서 severity로 완화/강화
        };
    }

    /**
     * severity를 반영해 baseMultiplyBy를 강화/완화
     * - CRITICAL: 무조건 차단(0)
     * - HIGH: 더 회피(더 작게)
     * - MEDIUM: 그대로
     * - LOW: 덜 회피(조금 크게)
     */
    public double applySeverityToMultiplyBy(double base, Severity severity) {
        if (base <= 0.0) return 0.0;
        if (severity == null) return clamp01(base);

        double v = switch (severity) {
            case CRITICAL -> 0.0;
            case HIGH -> base * 0.7;
            case MEDIUM -> base;
            case LOW -> base * 1.2;
        };
        return clamp01(v);
    }

    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
