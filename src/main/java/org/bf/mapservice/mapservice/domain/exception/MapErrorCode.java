package org.bf.mapservice.mapservice.domain.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bf.global.infrastructure.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MapErrorCode implements BaseErrorCode {
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "MAP404_1", "경로를 찾지 못했습니다."),
    ROUTE_NOT_SUITABLE_MOBILITY(HttpStatus.NOT_FOUND, "MAP404_2", "요청한 이동 수단으로는 경로가 없습니다."),
    ROUTE_OUT_OF_SERVICE_AREA(HttpStatus.BAD_REQUEST, "MAP400_1", "서비스 제공 지역을 벗어남"),
    ROUTE_ENGINE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"MAP500_1","경로 탐색 중 일시적인 오류가 발생했습니다."),
    ROUTE_ENGINE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "MAP504_1", "경로 엔진 응답이 지연되고 있습니다."),
    ROUTE_ENGINE_BUSY(HttpStatus.TOO_MANY_REQUESTS, "MAP429_1", "요청이 많아 잠시 후 다시 시도해 주세요."),
    ROUTE_ENGINE_UNREACHABLE(HttpStatus.SERVICE_UNAVAILABLE, "MAP503_1", "경로 엔진에 연결할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
