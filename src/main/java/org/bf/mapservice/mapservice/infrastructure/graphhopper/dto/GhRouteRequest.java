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
        Boolean lmDisable          // {"disable": true}
) {}
