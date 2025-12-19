package org.bf.mapservice.mapservice.infrastructure.graphhopper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class GraphHopperHttpClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GraphHopperHttpClient(
            @Value("${graphhopper.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     *  서비스용: POST /route (custom_model, areas, ch.disable 등 적용 가능)
     */
    public GhRouteResponse routePost(GhRouteRequest request) {
        log.info("[GH] POST /route profile={} points={} custom_model={} ch.disable={} lm.disable={}",
                request.profile(),
                request.points() != null ? request.points().size() : 0,
                request.custom_model() != null,
                request.chDisable(),
                request.lmDisable());

        try {
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            log.info("[GH][REQUEST JSON]\n{}", json);

        } catch (Exception e) {
            log.warn("[GH] Failed to serialize request", e);
        }

        GhRouteResponse res = restClient.post()
                .uri("/route")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(s -> s.isError(), (req, resp) -> {
                    String body = new String(resp.getBody().readAllBytes());
                    log.error("[GH] ERROR status={} body={}", resp.getStatusCode(), body);
                })
                .body(GhRouteResponse.class);
        log.info("[GH] request={}", request);

        if (res != null && res.paths() != null && !res.paths().isEmpty()) {
            var p = res.paths().get(0);
            log.info("[GH] OK distance={}m time={}ms", p.distance(), p.time());
        } else {
            log.warn("[GH] EMPTY RESPONSE");
        }
        return res;
    }

    /**
     * (디버그/임시용) GET /route
     * - avoid 같은 query param 테스트에만 사용 권장
     */
    public GhRouteResponse routeGet(
            String profile,
            double startLat, double startLon,
            double endLat, double endLon,
            String avoid,
            boolean chDisable
    ) {
        log.info("[GH] GET /route profile={} avoid={} ch.disable={} start=({}, {}) end=({}, {})",
                profile, avoid, chDisable, startLat, startLon, endLat, endLon);

        GhRouteResponse res = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/route")
                            .queryParam("profile", profile)
                            .queryParam("points_encoded", false)
                            // GH GET point=lat,lon
                            .queryParam("point", startLat + "," + startLon)
                            .queryParam("point", endLat + "," + endLon);

                    if (avoid != null && !avoid.isBlank()) {
                        b = b.queryParam("avoid", avoid);
                    }
                    if (chDisable) {
                        b = b.queryParam("ch.disable", true);
                    }
                    return b.build();
                })
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

    public GhRouteResponse routeGet(String profile, double startLat, double startLon, double endLat, double endLon) {
        return routeGet(profile, startLat, startLon, endLat, endLon, null, false);
    }
}
