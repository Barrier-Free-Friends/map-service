package org.bf.mapservice.mapservice.presentation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "장애물 GeoJSON FeatureCollection")
public record ObstacleFeatureCollectionDto(

        @Schema(description = "GeoJSON 타입", example = "FeatureCollection")
        String type,

        @Schema(description = "GeoJSON Feature 배열")
        List<Map<String, Object>> features
) {}
