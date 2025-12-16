package org.bf.mapservice.mapservice.kafkatest;

import org.bf.global.domain.event.DomainEventBuilder;
import org.bf.global.domain.event.EventPublisher;
import org.bf.mapservice.mapservice.infrastructure.kafka.KafkaTestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = KafkaTestConfig.class)
class KafkaTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private DomainEventBuilder eventBuilder;

    @Test
    void shouldPublishEventToRealKafkaBroker() throws InterruptedException {
        KafkaTestEvent rawEvent = new KafkaTestEvent("test");
        KafkaTestEvent event = eventBuilder.build(rawEvent);

        eventPublisher.publish(event);
        Thread.sleep(1000);
    }
}
