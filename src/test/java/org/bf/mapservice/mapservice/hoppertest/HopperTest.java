package org.bf.mapservice.mapservice.hoppertest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bf.mapservice.mapservice.infrastructure.graphhopper.dto.GhRouteRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class HopperTest {
    @Test
    void ghRouteRequest_json_serialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GhRouteRequest req = new GhRouteRequest(
                "foot_custom",
                List.of(
                        List.of(127.0, 37.5),
                        List.of(127.1, 37.6)
                ),
                false,
                false,
                Map.of("priority", List.of()),
                true,
                true,
                List.of("motorway", "ferry") // ✅ 1차는 엄격하게(원하면 빈 리스트로 시작 가능)

        );

        String json = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(req);

        System.out.println(json);
    }

}
