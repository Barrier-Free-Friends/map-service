package org.bf.mapservice.mapservice.presentation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.bf.mapservice.mapservice.domain.entity.Severity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "장애물 생성 요청 DTO")
public record CreateObstacleRequestDto(

        @NotNull
        @Schema(description = "지오메트리 타입", example = "POINT", allowableValues = {"POINT","LINESTRING"})
        ObstacleGeometryType geomType,

        @NotNull
        @Schema(description = "장애물 종류", example = "CONSTRUCTION")
        ObstacleType type,

        @NotNull
        @Schema(description = "위험도", example = "HIGH")
        Severity severity,

        @Schema(description = "POINT일 때 [lon,lat]", example = "[127.0055, 37.5055]")
        List<Double> point,

        @Schema(description = "LINESTRING일 때 [[lon,lat],...]", example = "[[127.001,37.501],[127.01,37.51]]")
        List<List<Double>> line,

        @Schema(description = "영향 반경(미터)", example = "30")
        Integer radiusMeters,

        @Schema(description = "시작 시각(ISO-8601)", example = "2025-12-18T08:00:00+09:00")
        OffsetDateTime startsAt,

        @Schema(description = "종료 시각(ISO-8601)", example = "2025-12-18T18:00:00+09:00")
        OffsetDateTime endsAt,

        @Schema(description = "장애물 등록한 유저의 ID", example = "UUID")
        UUID userId
) {}
