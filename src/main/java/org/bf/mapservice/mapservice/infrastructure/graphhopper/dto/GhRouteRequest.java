package org.bf.mapservice.mapservice.infrastructure.graphhopper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record GhRouteRequest(
        String profile,
        List<List<Double>> points,      // [[lon,lat], ...]
        boolean points_encoded,
        boolean instructions,
        Map<String, Object> custom_model,

        @JsonProperty("ch.disable")
        Boolean chDisable,

        @JsonProperty("lm.disable")
        Boolean lmDisable,

        //추가: 스냅 제약 (GraphHopper JSON 필드명 그대로)
        List<String> snap_preventions
) {
    /**  PointNotFound일 때만: 스냅 제약 완화(좌표는 그대로) */
    public GhRouteRequest relaxedSnap() {
        return new GhRouteRequest(
                profile,
                points,
                points_encoded,
                instructions,
                custom_model,
                chDisable,
                lmDisable,
                List.of() // 제약 제거
        );
    }
}