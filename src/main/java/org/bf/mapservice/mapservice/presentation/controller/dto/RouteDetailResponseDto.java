package org.bf.mapservice.mapservice.presentation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "길찾기 상세 응답")
public record RouteDetailResponseDto(

        @Schema(description = "총 이동 거리(미터)", example = "1325.4")
        double totalDistanceMeters,

        @Schema(description = "경로 GeoJSON")
        RouteGeoJsonResponseDto route,

        @Schema(description = "경로 구성 엣지 목록")
        List<RouteEdgeDto> edges,

        @Schema(description = "전체 경로 접근 가능 여부", example = "true")
        boolean fullyAccessible,

        @Schema(description = "접근 가능이 끊긴 엣지 순번(없으면 null)", example = "5")
        Integer accessibleUntilSeq,

        @Schema(description = "처음 차단된 사유(없으면 null)", example = "계단 구간 존재")
        String firstBlockedReason,

        @Schema(description = "요청한 이동수단 타입", example = "WHEELCHAIR")
        String requestedMobilityType
) {}
