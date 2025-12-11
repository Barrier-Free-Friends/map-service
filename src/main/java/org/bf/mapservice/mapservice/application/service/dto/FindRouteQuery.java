package org.bf.mapservice.mapservice.application.service.dto;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;

public record FindRouteQuery(
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        MobilityType mobilityType
) {
}