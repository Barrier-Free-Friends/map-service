package org.bf.mapservice.mapservice.presentation.controller;

import jakarta.validation.Valid;
import org.bf.mapservice.mapservice.application.command.CreateObstacleCommand;
import org.bf.mapservice.mapservice.application.command.ObstacleCommandService;
import org.bf.mapservice.mapservice.domain.entity.ObstacleGeometryType;
import org.bf.mapservice.mapservice.presentation.controller.dto.CreateObstacleRequestDto;
import org.locationtech.jts.geom.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/obstacles")
public class ObstaclesController {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final ObstacleCommandService commandService;

    public ObstaclesController(ObstacleCommandService commandService) {
        this.commandService = commandService;
    }

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