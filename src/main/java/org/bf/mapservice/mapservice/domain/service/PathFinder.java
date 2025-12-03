package org.bf.mapservice.mapservice.domain.service;

import org.bf.mapservice.mapservice.domain.entity.MobilityProfile;

import java.util.List;

public interface PathFinder {

    List<Long> findPath(
            Long startNodeId,
            Long endNodeId,
            MobilityProfile profile
    );
}
