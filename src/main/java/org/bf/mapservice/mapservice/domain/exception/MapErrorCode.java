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

    OBSTACLE_NOT_FOUND(HttpStatus.NOT_FOUND, "MAP404_3", "요청한 이동 수단으로는 경로가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
