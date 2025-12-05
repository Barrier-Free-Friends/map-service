package org.bf.mapservice.mapservice.presentation.controller;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;

import java.util.List;

public record RouteRequest(
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        MobilityType mobilityType
) {
}

