package org.bf.mapservice.mapservice.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.application.command.CreateObstacleCommand;
import org.bf.mapservice.mapservice.application.command.ObstacleCommandService;
import org.bf.mapservice.mapservice.application.query.ObstacleCustomModelBuilder;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.infrastructure.persistence.ObstacleQueryDaoImpl;
import org.bf.mapservice.mapservice.presentation.controller.dto.CreateObstacleRequestDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.ObstacleFeatureCollectionDto;
import org.locationtech.jts.geom.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/obstacles")
@RequiredArgsConstructor
public class ObstaclesController {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final ObstacleCommandService commandService;
    private final ObstacleQueryDaoImpl obstacleQueryDaoImpl;
    private final ObstacleCustomModelBuilder obstacleCustomModelBuilder;

    @Operation(summary = "장애물 설정", description = "위도, 경도, 장애물 타입을 이용하여 장애물 설정")
    @PostMapping
    public Long create(@RequestBody @Valid CreateObstacleRequestDto req) {
        Geometry geom = toGeometry(req);

        var cmd = new CreateObstacleCommand(
                geom,
                req.geomType(),
                req.type(),
                req.severity(),
                req.radiusMeters(),
                req.startsAt(),
                req.endsAt()
        );
        return commandService.create(cmd);
    }

    @Operation(summary = "장애물 해결(삭제 처리)", description = "id에 해당하는 장애물을 RESOLVED 처리합니다.")
    @PutMapping("/{id}/resolve")
    public void resolve(
            @Parameter(description = "장애물 ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long id
    ) {
        commandService.resolve(id);
    }

    @Operation(summary = "활성 장애물 조회", description = "Envelope(minLon,minLat,maxLon,maxLat) 안의 ACTIVE 장애물을 GeoJSON FeatureCollection 형태로 반환")
    @GetMapping
    public ObstacleFeatureCollectionDto getActiveObstacles(
            @Parameter(description = "최소 경도", required = true) @RequestParam Double minLon,
            @Parameter(description = "최소 위도", required = true) @RequestParam Double minLat,
            @Parameter(description = "최대 경도", required = true) @RequestParam Double maxLon,
            @Parameter(description = "최대 위도", required = true) @RequestParam Double maxLat
    ) {
        var obstacles = obstacleQueryDaoImpl.findActiveObstaclesInEnvelope(
                minLon, minLat, maxLon, maxLat,
                OffsetDateTime.now()
        );

        // FeatureCollection(Map)에서 features만 꺼내서 내려줌
        Map<String, Object> fc = obstacleCustomModelBuilder.buildAreasOnly(obstacles);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features = (List<Map<String, Object>>) fc.getOrDefault("features", List.of());

        return new ObstacleFeatureCollectionDto("FeatureCollection", features);
    }

    private Geometry toGeometry(CreateObstacleRequestDto req) {
        if (req.geomType() == ObstacleGeometryType.POINT) {
            List<Double> p = req.point();
            if (p == null || p.size() != 2) throw new IllegalArgumentException("POINT requires [lon,lat]");
            return GF.createPoint(new Coordinate(p.get(0), p.get(1)));
        }

        if (req.geomType() == ObstacleGeometryType.LINESTRING) {
            List<List<Double>> line = req.line();
            if (line == null || line.size() < 2) throw new IllegalArgumentException("LINESTRING requires at least 2 points");
            Coordinate[] coords = line.stream()
                    .map(x -> new Coordinate(x.get(0), x.get(1)))
                    .toArray(Coordinate[]::new);
            return GF.createLineString(coords);
        }

        throw new IllegalArgumentException("Unsupported geomType");
    }
}