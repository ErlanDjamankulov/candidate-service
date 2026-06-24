package kg.tunduk.cvscan.candidate.messaging;

import kg.tunduk.cvscan.candidate.dto.event.StatusChangedEvent;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class StatusChangedProducer {

    private static final Logger log = LoggerFactory.getLogger(StatusChangedProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public StatusChangedProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.candidate-status-changed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(String candidateId, CandidateStatus from, CandidateStatus to) {
        StatusChangedEvent event = new StatusChangedEvent(
                UUID.randomUUID(),
                candidateId,
                from,
                to,
                OffsetDateTime.now()
        );
        kafkaTemplate.send(topic, candidateId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Не удалось опубликовать событие смены статуса для кандидата {}: {}",
                                candidateId, ex.getMessage(), ex);
                    } else {
                        log.debug("Опубликовано событие смены статуса {} -> {} для кандидата {}", from, to, candidateId);
                    }
                });
    }
}