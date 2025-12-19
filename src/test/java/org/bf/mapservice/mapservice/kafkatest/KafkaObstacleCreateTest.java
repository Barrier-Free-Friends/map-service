package org.bf.mapservice.mapservice.kafkatest;

import org.bf.global.domain.event.DomainEventBuilder;
import org.bf.global.domain.event.EventPublisher;
import org.bf.global.infrastructure.event.ReportMapImageInfo;
import org.bf.global.infrastructure.event.ReportMapInfoEvent;
import org.bf.mapservice.mapservice.infrastructure.repository.ObstacleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KafkaObstacleCreateTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private DomainEventBuilder eventBuilder;

    @Test
    void shouldPublishReportMapInfoEvent() throws InterruptedException {

        // 1) 순수 payload 이벤트 생성 (메타데이터는 아직 없음)
        ReportMapInfoEvent rawEvent = new ReportMapInfoEvent();
            rawEvent.setUserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        rawEvent.setTagCode("tree");
        rawEvent.setImages(List.of(
                new ReportMapImageInfo(37.48930, 127.03525)
        ));

        // 2) 빌더로 공통 메타데이터 주입 (eventId/occurredAt/sourceService)
        ReportMapInfoEvent event = eventBuilder.build(rawEvent);

        // 3) 발행
        eventPublisher.publish(event);

        // 비동기 전송/컨슘 처리까지 잠깐 대기 (원래는 Awaitility 추천)
        Thread.sleep(1000);
    }
}

