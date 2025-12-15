package org.bf.mapservice.mapservice.presentation.controller.dto;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;

public record RouteRequestDto(
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        MobilityType mobilityType
) {}