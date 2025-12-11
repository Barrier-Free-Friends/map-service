package org.bf.mapservice.mapservice.application.service.query;

import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.infrastructure.persistence.RoutingRepository;
import org.bf.mapservice.mapservice.presentation.controller.dto.NearestNodeResponseDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapNodeQueryService {

    private final RoutingRepository routingRepository;

    public NearestNodeResponseDto findNearestNode(double lat, double lng) {
        return routingRepository.findNearestVertexWithDistance(lat, lng);
    }
}
