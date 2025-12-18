package org.bf.mapservice.mapservice.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bf.mapservice.mapservice.application.command.CreateObstacleCommand;
import org.bf.mapservice.mapservice.application.command.ObstacleCommandService;
import org.bf.mapservice.mapservice.application.query.ObstacleCustomModelBuilder;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.infrastructure.persistence.ObstacleQueryDaoImpl;
import org.bf.mapservice.mapservice.presentation.controller.dto.CreateObstacleRequestDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.ObstacleFeatureCollectionDto;
import org.bf.mapservice.mapservice.presentation.docs.ObstaclesApiDoc;
import org.locationtech.jts.geom.*;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/obstacles")
@RequiredArgsConstructor
public class ObstaclesController implements ObstaclesApiDoc {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final ObstacleCommandService commandService;
    private final ObstacleQueryDaoImpl obstacleQueryDaoImpl;
    private final ObstacleCustomModelBuilder obstacleCustomModelBuilder;

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

    @PutMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id) {
        commandService.resolve(id);
    }

    @GetMapping
    public ObstacleFeatureCollectionDto getActiveObstacles(
        @RequestParam Double minLon,
        @RequestParam Double minLat,
        @RequestParam Double maxLon,
        @RequestParam Double maxLat
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