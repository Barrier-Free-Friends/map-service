package org.bf.mapservice.mapservice.infrastructure.graphhopper;

import lombok.extern.slf4j.Slf4j;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GraphHopperHttpClient {

    private final RestClient restClient;

    public GraphHopperHttpClient(@Value("${graphhopper.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public GhRouteResponse routeGet(String profile, double startLat, double startLon, double endLat, double endLon) {
        log.info("[GH] GET /route profile={} start=({}, {}) end=({}, {})",
                profile, startLat, startLon, endLat, endLon);

        GhRouteResponse res = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route")
                        .queryParam("profile", profile)
                        .queryParam("points_encoded", false)
                        // GH GET point=lat,lon 순서
                        .queryParam("point", startLat + "," + startLon)
                        .queryParam("point", endLat + "," + endLon)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GhRouteResponse.class);

        if (res != null && res.paths() != null && !res.paths().isEmpty()) {
            var p = res.paths().get(0);
            log.info("[GH] OK distance={}m time={}ms", p.distance(), p.time());
        } else {
            log.warn("[GH] EMPTY RESPONSE");
        }
        return res;
    }
}
