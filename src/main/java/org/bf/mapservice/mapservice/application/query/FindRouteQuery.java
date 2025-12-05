package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;

public record FindRouteQuery(
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        MobilityType mobilityType
) {
}