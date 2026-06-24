package kg.tunduk.cvscan.candidate.messaging;

import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.service.CandidateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CvParsedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CvParsedConsumer.class);

    private final CandidateService candidateService;

    public CvParsedConsumer(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @KafkaListener(topics = "${app.kafka.topics.cv-parsed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(CvParsedEvent event) {
        if (event == null || event.candidateId() == null) {
            log.warn("Получено некорректное событие cv.parsed без candidateId, пропускаем: {}", event);
            return;
        }

        boolean created = candidateService.createFromParsedEvent(event);

        if (created) {
            log.info("Кандидат '{}' создан из события cv.parsed (eventId={})",
                    event.candidateId(), event.eventId());
        } else {
            log.info("Событие cv.parsed для кандидата '{}' проигнорировано — уже существует (eventId={})",
                    event.candidateId(), event.eventId());
        }
    }
}