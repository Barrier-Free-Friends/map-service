package org.bf.mapservice.mapservice.presentation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;

@Schema(description = "길찾기 요청 DTO")
public record RouteRequestDto(

        @Schema(description = "출발 위도", example = "37.501")
        Double startLatitude,

        @Schema(description = "출발 경도", example = "127.001")
        Double startLongitude,

        @Schema(description = "도착 위도", example = "37.510")
        Double endLatitude,

        @Schema(description = "도착 경도", example = "127.010")
        Double endLongitude,

        @Schema(
                description = "이동수단 타입",
                example = "WHEELCHAIR",
                allowableValues = {"PEDESTRIAN", "WHEELCHAIR"}
        )
        MobilityType mobilityType
) {}
