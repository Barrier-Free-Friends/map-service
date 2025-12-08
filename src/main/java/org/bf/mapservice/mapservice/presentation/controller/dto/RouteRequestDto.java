package org.bf.mapservice.mapservice.presentation.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;

public record RouteRequestDto(
        @NotNull(message = "startLatitude는 필수입니다.")
        Double startLatitude,

        @NotNull(message = "startLongitude는 필수입니다.")
        Double startLongitude,

        @NotNull(message = "endLatitude는 필수입니다.")
        Double endLatitude,

        @NotNull(message = "endLongitude는 필수입니다.")
        Double endLongitude,

        @NotNull(message = "mobilityType은 필수입니다.")
        MobilityType mobilityType
) { }
