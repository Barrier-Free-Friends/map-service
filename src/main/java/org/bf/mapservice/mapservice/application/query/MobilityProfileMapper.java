package org.bf.mapservice.mapservice.application.query;

import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.springframework.stereotype.Component;

@Component
public class MobilityProfileMapper {
    public String toGhProfile(MobilityType type) {
        return switch (type) {
            case PEDESTRIAN -> "foot";
            case WHEELCHAIR, STROLLER, ELDERLY -> "wheelchair";
        };
    }
}