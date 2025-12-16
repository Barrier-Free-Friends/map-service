package org.bf.mapservice.mapservice.kafkatest;

import org.awaitility.Awaitility;
import org.bf.mapservice.mapservice.infrastructure.repository.ObstacleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KafkaTestConfig.class)
class KafkaObstacleCreateTest {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldCreateObstacleByKafkaMessage() {

        String json = """
                {
                  "userId": "11111111-1111-1111-1111-111111111111",
                  "images": [
                    { "latitude": 37.48930, "longitude": 127.03525, "address": "ignored" }
                  ]
                }
        """;

        kafkaTemplate.send("map-events", json);

    }
}
