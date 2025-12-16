package org.bf.mapservice.mapservice.presentation.controller.dto;

import java.util.List;
import java.util.Map;

public record ObstacleFeatureCollectionDto(
        String type, // "FeatureCollection"
        List<Map<String, Object>> features
) {}
