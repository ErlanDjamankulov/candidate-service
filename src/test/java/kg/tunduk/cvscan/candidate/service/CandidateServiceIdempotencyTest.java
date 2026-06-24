package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.messaging.StatusChangedProducer;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceIdempotencyTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private StatusService statusService;

    @Mock
    private StatusChangedProducer statusChangedProducer;

    private CandidateService candidateService;

    private static final OffsetDateTime PARSED_AT = OffsetDateTime.parse("2026-05-10T09:00:00Z");

    @BeforeEach
    void setUp() {
        candidateService = new CandidateService(
                candidateRepository, statusService, new CandidateMapper(), statusChangedProducer);
    }

    private CvParsedEvent event(String candidateId, OffsetDateTime parsedAt) {
        return new CvParsedEvent(
                UUID.randomUUID(), candidateId, parsedAt, "Асанов Бакыт", "java-middle",
                "Java — ведущий программист", "asanov.bakyt@email.com", "+996 700 111222",
                "Бишкек", "@asanov_dev", "~4 г.", "Java 17, Spring Boot 3", "КГТУ, 2021",
                Verdict.FIT, "summary", List.of(), List.of(), List.of()
        );
    }

    @Test
    @DisplayName("Новый candidateId — кандидат создаётся")
    void newCandidateId_isCreated() {
        when(candidateRepository.existsById("asanov-bakyt")).thenReturn(false);

        boolean created = candidateService.createFromParsedEvent(event("asanov-bakyt", PARSED_AT));

        assertThat(created).isTrue();
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Повтор того же candidateId + parsedAt — дубль не создаётся")
    void sameCandidateIdAndParsedAt_isIgnored() {
        when(candidateRepository.existsById("asanov-bakyt")).thenReturn(true);

        boolean created = candidateService.createFromParsedEvent(event("asanov-bakyt", PARSED_AT));

        assertThat(created).isFalse();
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Тот же candidateId с другим parsedAt — тоже не создаёт новую запись")
    void sameCandidateIdDifferentParsedAt_stillNotDuplicated() {
        when(candidateRepository.existsById("asanov-bakyt")).thenReturn(true);

        OffsetDateTime laterParsedAt = PARSED_AT.plusDays(1);
        boolean created = candidateService.createFromParsedEvent(event("asanov-bakyt", laterParsedAt));

        assertThat(created).isFalse();
        verify(candidateRepository, never()).save(any(Candidate.class));
    }
}