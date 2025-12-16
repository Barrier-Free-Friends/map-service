package org.bf.mapservice.mapservice.domain.entity;

public enum MobilityType {

    // 일반 도보 사용자
    PEDESTRIAN,

    // 휠체어 사용자
    WHEELCHAIR,

    // 유모차 사용자
    STROLLER,

    // 노약자
    ELDERLY;

    // 계단을 물리적으로 이용 가능한가?
    public boolean canUseStairs() {
        return this == PEDESTRIAN;
    }

    // 엘리베이터/경사/보행환경에 민감한가?
    public boolean isBarrierSensitive() {
        return this != PEDESTRIAN;
    }

    /**
     * 장애물 영향 강도 기본값
     * 1.0 = 일반
     * < 1.0 = 장애물에 더 민감
     */
    public double basePenaltyFactor() {
        return switch (this) {
            case PEDESTRIAN -> 1.0;
            case STROLLER -> 0.8;
            case ELDERLY -> 0.6;
            case WHEELCHAIR -> 0.4;
        };
    }
}