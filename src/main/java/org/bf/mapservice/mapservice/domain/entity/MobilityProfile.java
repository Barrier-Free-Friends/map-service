package org.bf.mapservice.mapservice.domain.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
public class MobilityProfile {

    private final boolean avoidStairs;
    private final boolean avoidHighSlope;
    private final boolean allowVehicleRoad;

    // 추후 필요 시 더 추가 가능 (예: maxSlopePercent 등)

    @Builder
    private MobilityProfile(
            boolean avoidStairs,
            boolean avoidHighSlope,
            boolean allowVehicleRoad
    ) {
        this.avoidStairs = avoidStairs;
        this.avoidHighSlope = avoidHighSlope;
        this.allowVehicleRoad = allowVehicleRoad;
    }

    public static MobilityProfile from(MobilityType mobilityType) {
        return switch (mobilityType) {
            case WHEELCHAIR -> MobilityProfile.builder()
                    .avoidStairs(true)
                    .avoidHighSlope(true)
                    .allowVehicleRoad(false)
                    .build();
            case ELDERLY -> MobilityProfile.builder()
                    .avoidStairs(true)
                    .avoidHighSlope(true)
                    .allowVehicleRoad(false)
                    .build();
            case STROLLER -> MobilityProfile.builder()
                    .avoidStairs(true)
                    .avoidHighSlope(false)
                    .allowVehicleRoad(false)
                    .build();
        };
    }
}
