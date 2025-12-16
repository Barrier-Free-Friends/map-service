package org.bf.mapservice.mapservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.bf.global.infrastructure.event.ReportMapImageInfo;
import org.bf.global.infrastructure.event.ReportMapInfoEvent;
import org.bf.mapservice.mapservice.application.command.CreateObstacleCommand;
import org.bf.mapservice.mapservice.application.command.ObstacleCommandService;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.domain.entity.ObstacleType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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

        if (event.getUserId() == null) return;
        if (event.getImages() == null || event.getImages().isEmpty()) return;

        // tagCode 기반으로 타입 결정 (없거나 모르면 OTHER_OBSTACLE)
        ObstacleType type = mapTagCodeToObstacleType(event.getTagCode());

        for (ReportMapImageInfo img : event.getImages()) {
            if (img == null || img.latitude() == null || img.longitude() == null) continue;

            var point = geometryFactory.createPoint(
                    new Coordinate(img.longitude(), img.latitude()) // lon, lat
            );

            // severity / radius는 백엔드(ObstacleCommandService)에서 자동 결정
            CreateObstacleCommand cmd = new CreateObstacleCommand(
                    point,
                    ObstacleGeometryType.POINT,
                    type,
                    null,   // severity -> create()에서 defaults로 보정
                    0,      // radius   -> create()에서 defaults로 보정
                    null,
                    null
            );

            commandService.create(cmd);
        }
    }

    private ObstacleType mapTagCodeToObstacleType(String tagCode) {
        if (tagCode == null || tagCode.isBlank()) return ObstacleType.OTHER_OBSTACLE;

        // 1) 송신자가 enum 이름을 그대로 보내는 경우: TREE, ROCK, ...
        try {
            return ObstacleType.valueOf(tagCode.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // 2) 송신자가 별도 코드로 보내는 경우는 여기서 매핑
            // 예: "CONSTR" -> CONSTRUCTION
            return switch (tagCode.trim().toUpperCase()) {
                case "CONSTR", "CONSTRUCTION" -> ObstacleType.CONSTRUCTION;
                case "ETC", "OTHER" -> ObstacleType.OTHER_OBSTACLE;
                default -> ObstacleType.OTHER_OBSTACLE;
            };
        }
    }
}
