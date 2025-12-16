package org.bf.mapservice.mapservice.infrastructure.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bf.global.domain.event.AbstractDomainEvent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTestEvent extends AbstractDomainEvent {

    // 이벤트 메시지 발행 시 수신자에게 전달해야 하는 필드들 정의
    private String content;

    // 필수 구현 (수신자와 협의한 topic name 기입)
    @Override
    public String getTopicName() {
        return "map-events";
    }
}