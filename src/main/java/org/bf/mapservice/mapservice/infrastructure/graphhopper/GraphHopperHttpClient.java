package org.bf.mapservice.mapservice.infrastructure.graphhopper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bf.global.infrastructure.exception.CustomException;
import org.bf.mapservice.mapservice.domain.exception.MapErrorCode;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class GraphHopperHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // ✅ PointNotFound만 식별용 내부 예외
    private static class GhPointNotFound extends RuntimeException { }

    public GraphHopperHttpClient(
            @Value("${graphhopper.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        this.objectMapper = objectMapper;
    }

    public GhRouteResponse routePost(GhRouteRequest request) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            // 1차 시도
            return doCall(requestId, request);

        } catch (GhPointNotFound pnfe) {
            // ✅ 여기만 추가: PointNotFound면 스냅 제약 완화해서 1회 재시도
            log.info("[GH][RETRY][{}] PointNotFound -> retry with relaxed snap", requestId);
            return doCall(requestId, request.relaxedSnap());

        } finally {
            MDC.clear();
        }
    }

    private GhRouteResponse doCall(String requestId, GhRouteRequest request) {
        long startNs = System.nanoTime();

        try {
            log.info("[GH][REQ][{}] profile={} points={} custom_model={} ch.disable={} lm.disable={} snap_preventions={}",
                    requestId,
                    request.profile(),
                    request.points() != null ? request.points().size() : 0,
                    request.custom_model() != null,
                    request.chDisable(),
                    request.lmDisable(),
                    request.snap_preventions()
            );

            return restClient.post()
                    .uri("/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, resp) -> {
                        int status = resp.getStatusCode().value();
                        byte[] bytes = resp.getBody().readAllBytes();
                        String body = new String(bytes, StandardCharsets.UTF_8);

                        if (status >= 400) {
                            log.error("[GH][HTTP_ERROR][{}] status={} body={}",
                                    requestId, status, trim(body, 1200));
                            throw mapGhErrorToDomain(status, body); // 여기서 GhPointNotFound 던질 수 있음
                        }

                        GhRouteResponse res = objectMapper.readValue(bytes, GhRouteResponse.class);

                        if (res == null || res.paths() == null || res.paths().isEmpty()) {
                            log.warn("[GH][NO_PATH][{}] profile={} points={}",
                                    requestId, request.profile(), request.points());
                            throw new CustomException(MapErrorCode.ROUTE_NOT_SUITABLE_MOBILITY);
                        }

                        log.info("[GH][OK][{}] took={}ms", requestId, tookMs(startNs));
                        return res;
                    });

        } catch (CustomException e) {
            log.warn("[GH][DOMAIN_ERROR][{}] took={}ms message={}", requestId, tookMs(startNs), e.getMessage());
            throw e;

        } catch (ResourceAccessException e) {
            Throwable root = rootCause(e);
            log.error("[GH][IO_ERROR][{}] took={}ms root={} msg={}",
                    requestId, tookMs(startNs), root.getClass().getName(), root.getMessage());

            if (root instanceof SocketTimeoutException) throw new CustomException(MapErrorCode.ROUTE_ENGINE_TIMEOUT);
            if (root instanceof ConnectException) throw new CustomException(MapErrorCode.ROUTE_ENGINE_UNREACHABLE);
            if (root instanceof SocketException) throw new CustomException(MapErrorCode.ROUTE_ENGINE_ERROR);

            throw new CustomException(MapErrorCode.ROUTE_ENGINE_ERROR);

        } catch (GhPointNotFound e) {
            // ✅ routePost에서 잡아서 재시도하도록 그대로 던짐
            throw e;

        } catch (Exception e) {
            log.error("[GH][SYSTEM_ERROR][{}] took={}ms", requestId, tookMs(startNs), e);
            throw new CustomException(MapErrorCode.ROUTE_ENGINE_ERROR);
        }
    }

    private RuntimeException mapGhErrorToDomain(int status, String body) {
        if (body.contains("PointOutOfBoundsException")) {
            return new CustomException(MapErrorCode.ROUTE_OUT_OF_SERVICE_AREA);
        }
        if (body.contains("PointNotFoundException")) {
            // ✅ 여기서만 특별 처리: 재시도 트리거
            return new GhPointNotFound();
        }

        if (status >= 500) return new CustomException(MapErrorCode.ROUTE_ENGINE_ERROR);
        return new CustomException(MapErrorCode.ROUTE_NOT_FOUND);
    }

    private static long tookMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
