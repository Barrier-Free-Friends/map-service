package org.bf.mapservice.mapservice.presentation.controller;

import java.util.List;

public record RouteResponse(
        List<Long> nodeIds
) {
}
