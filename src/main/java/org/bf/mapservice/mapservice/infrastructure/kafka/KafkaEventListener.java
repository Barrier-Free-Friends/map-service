package org.bf.mapservice.mapservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bf.global.infrastructure.event.ReportMapImageInfo;
import org.bf.global.infrastructure.event.ReportMapInfoEvent;
import org.bf.mapservice.mapservice.application.command.CreateObstacleCommandDto;
import org.bf.mapservice.mapservice.application.command.ObstacleCommandService;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final ObstacleCommandService commandService;

    // SRID = 4326
    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    @KafkaListener(
            topics = "map-events",
            groupId = "mapservice-group",
            containerFactory = "genericKafkaListenerContainerFactory"
    )
    public void handleReportMapInfo(ReportMapInfoEvent event) {

        if (event.getUserId() == null) {
            return;
        }

        if (event.getImages() == null || event.getImages().isEmpty()) {
            return;
        }

        ObstacleType type = mapTagCodeToObstacleType(event.getTagCode());
        UUID userId = event.getUserId();

        for (ReportMapImageInfo img : event.getImages()) {
            if (img == null || img.latitude() == null || img.longitude() == null) {
                continue;
            }

            var point = geometryFactory.createPoint(
                    new Coordinate(img.longitude(), img.latitude())
            );

            CreateObstacleCommandDto cmd = new CreateObstacleCommandDto(
                    point,
                    ObstacleGeometryType.POINT,
                    type,
                    null,
                    null,
                    null,
                    null,
                    userId
            );

            commandService.create(cmd);
        }
    }

    private ObstacleType mapTagCodeToObstacleType(String tagCode) {
        if (tagCode == null || tagCode.isBlank()) return ObstacleType.OTHER_OBSTACLE;

        try {
            return ObstacleType.valueOf(tagCode.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return switch (tagCode.trim().toUpperCase()) {
                case "CONSTR", "CONSTRUCTION" -> ObstacleType.CONSTRUCTION;
                case "ETC", "OTHER" -> ObstacleType.OTHER_OBSTACLE;
                default -> ObstacleType.OTHER_OBSTACLE;
            };
        }
    }
}
