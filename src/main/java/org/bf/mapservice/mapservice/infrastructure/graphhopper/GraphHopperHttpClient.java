package org.bf.mapservice.mapservice.infrastructure.graphhopper;

import lombok.extern.slf4j.Slf4j;
import org.bf.global.infrastructure.exception.CustomException;
import org.bf.mapservice.mapservice.domain.exception.MapErrorCode;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class GraphHopperHttpClient {

    private final RestClient restClient;

    public GraphHopperHttpClient(
            @Value("${graphhopper.base-url}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(8_000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * 서비스용: POST /route (custom_model, areas, ch.disable 등 적용 가능)
     */
    public GhRouteResponse routePost(GhRouteRequest request) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            log.info("[GH][REQ] profile={} points={} chDisable={} lmDisable={}",
                    request.profile(),
                    request.points() != null ? request.points().size() : 0,
                    request.chDisable(),
                    request.lmDisable()
            );

            GhRouteResponse res = restClient.post()
                    .uri("/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String body = "";
                        try {
                            body = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        } catch (Exception ignored) {}

                        log.error("[GH][HTTP_ERROR] status={} body={}", resp.getStatusCode(), body);

                        // 여기서 반드시 throw로 흐름 종료
                        if (body.contains("PointOutOfBoundsException")) {
                            throw new CustomException(MapErrorCode.ROUTE_OUT_OF_SERVICE_AREA);
                        }
                        if (resp.getStatusCode().is5xxServerError()) {
                            throw new CustomException(MapErrorCode.ROUTE_ENGINE_ERROR);
                        }
                        throw new CustomException(MapErrorCode.ROUTE_NOT_FOUND);
                    })
                    .body(GhRouteResponse.class);

            // 정상 응답 but 경로 없음
            if (res == null || res.paths() == null || res.paths().isEmpty()) {
                log.warn("[GH][NO_PATH] profile={} points={}",
                        request.profile(), request.points());

                throw new CustomException(
                        MapErrorCode.ROUTE_NOT_SUITABLE_MOBILITY
                );
            }

            var p = res.paths().get(0);
            log.info("[GH][OK] distance={}m time={}ms",
                    p.distance(), p.time());

            return res;

        } catch (CustomException e) {
            // 도메인 의미가 있는 에러
            log.warn("[GH][DOMAIN_ERROR] {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // 네트워크 / 타임아웃 / 역직렬화
            log.error("[GH][SYSTEM_ERROR] profile={} points={}",
                    request.profile(), request.points(), e);

            throw new CustomException(
                    MapErrorCode.ROUTE_ENGINE_ERROR
            );
        } finally {
            MDC.clear();
        }
    }
}