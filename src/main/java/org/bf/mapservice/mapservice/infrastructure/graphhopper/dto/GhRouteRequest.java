package org.bf.mapservice.mapservice.infrastructure.graphhopper.dto;

import java.util.List;
import java.util.Map;

public record GhRouteRequest(
        String profile,
        List<List<Double>> points,      // [[lon,lat], ...]
        boolean points_encoded,
        boolean instructions,
        Map<String, Object> custom_model,
        Map<String, Object> ch          // {"disable": true}
) {}
