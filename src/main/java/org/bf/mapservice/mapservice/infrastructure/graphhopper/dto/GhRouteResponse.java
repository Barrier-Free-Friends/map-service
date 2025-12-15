package org.bf.mapservice.mapservice.infrastructure.graphhopper.dto;

import java.util.List;

public record GhRouteResponse(List<Path> paths) {

    public record Path(
            double distance,
            double weight,
            long time,
            Points points
    ) {}

    public record Points(
            String type,                    // "LineString"
            List<List<Double>> coordinates  // [[lon,lat], ...]
    ) {}
}
