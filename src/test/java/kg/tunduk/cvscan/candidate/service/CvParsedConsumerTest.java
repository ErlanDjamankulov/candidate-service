package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.messaging.CvParsedConsumer;
import kg.tunduk.cvscan.candidate.model.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvParsedConsumerTest {

    @Mock
    private CandidateService candidateService;

    private CvParsedConsumer consumer;

    private CvParsedEvent sampleEvent(UUID eventId) {
        return new CvParsedEvent(
                eventId,
                "asanov-bakyt",
                OffsetDateTime.parse("2026-05-10T09:00:00Z"),
                "Асанов Бакыт Эркинович",
                "java-middle",
                "Java — ведущий программист",
                "asanov.bakyt@email.com",
                "+996 700 111222",
                "Бишкек",
                "@asanov_dev",
                "~4 г.",
                "Java 17, Spring Boot 3, PostgreSQL, Kafka",
                "КГТУ, ИТ, 2021",
                Verdict.FIT,
                "Backend-разработчик с опытом",
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    @DisplayName("Первое событие создаёт кандидата")
    void firstEvent_createsCandidate() {
        consumer = new CvParsedConsumer(candidateService);
        CvParsedEvent event = sampleEvent(UUID.randomUUID());

        when(candidateService.createFromParsedEvent(event)).thenReturn(true);

        consumer.onMessage(event);

        verify(candidateService, times(1)).createFromParsedEvent(event);
    }

    @Test
    @DisplayName("Повторное событие с тем же candidateId + parsedAt не создаёт дубль")
    void duplicateEvent_doesNotCreateDuplicate() {
        consumer = new CvParsedConsumer(candidateService);
        CvParsedEvent original = sampleEvent(UUID.randomUUID());
        CvParsedEvent duplicate = sampleEvent(UUID.randomUUID()); // другой eventId, тот же candidateId+parsedAt

        when(candidateService.createFromParsedEvent(original)).thenReturn(true);
        when(candidateService.createFromParsedEvent(duplicate)).thenReturn(false);

        consumer.onMessage(original);
        consumer.onMessage(duplicate);

        verify(candidateService, times(1)).createFromParsedEvent(original);
        verify(candidateService, times(1)).createFromParsedEvent(duplicate);

    }

    @Test
    @DisplayName("Событие без candidateId игнорируется без вызова сервиса")
    void eventWithoutCandidateId_isSkipped() {
        consumer = new CvParsedConsumer(candidateService);
        CvParsedEvent invalid = new CvParsedEvent(
                UUID.randomUUID(), null, OffsetDateTime.now(), "Имя", "java-middle",
                null, "test@email.com", null, null, null, null, null, null,
                Verdict.FIT, null, List.of(), List.of(), List.of()
        );

        consumer.onMessage(invalid);

        verify(candidateService, never()).createFromParsedEvent(any());
    }
}
