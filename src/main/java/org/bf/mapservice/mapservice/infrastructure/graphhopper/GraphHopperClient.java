package org.bf.mapservice.mapservice.infrastructure.graphhopper;

import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;

public interface GraphHopperClient {
    GhRouteResponse route(GhRouteRequest request);
}