package kg.tunduk.cvscan.candidate.integration;

import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Перед каждым тестом БД сбрасывается до состояния первоначальных тестовых даннх, чтобы тесты не зависели друг от друга.
 */
@Sql(scripts = "/db/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class KafkaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private CandidateRepository candidateRepository;

    @Value("${app.kafka.topics.cv-parsed}")
    private String cvParsedTopic;

    private CvParsedEvent buildEvent(String candidateId, OffsetDateTime parsedAt, String email) {
        return new CvParsedEvent(
                UUID.randomUUID(),
                candidateId,
                parsedAt,
                "Тестовый Кандидат",
                "java-middle",
                "Java — ведущий программист",
                email,
                "+996 700 555444",
                "Бишкек",
                "@test_kafka",
                "~2 г.",
                "Java 17, Spring Boot, Kafka",
                "КНУ, 2022",
                Verdict.FIT,
                "Кандидат для проверки Kafka consumer",
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    void cvParsedEvent_consumedAndCandidateCreated() {
        String candidateId = "kafka-test-create";
        OffsetDateTime parsedAt = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        CvParsedEvent event = buildEvent(candidateId, parsedAt, "kafka.test.create@email.com");

        kafkaTemplate.send(cvParsedTopic, candidateId, event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(candidateRepository.findById(candidateId)).isPresent()
        );

        var saved = candidateRepository.findById(candidateId).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("kafka.test.create@email.com");
        assertThat(saved.getStatus().name()).isEqualTo("NEW");
        assertThat(saved.getVerdict()).isEqualTo(Verdict.FIT);
    }

    @Test
    void duplicateCvParsedEvent_doesNotCreateDuplicate() throws InterruptedException {
        String candidateId = "kafka-test-duplicate";
        OffsetDateTime parsedAt = OffsetDateTime.parse("2026-06-02T11:00:00Z");
        CvParsedEvent original = buildEvent(candidateId, parsedAt, "kafka.test.duplicate@email.com");

        kafkaTemplate.send(cvParsedTopic, candidateId, original);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(candidateRepository.findById(candidateId)).isPresent()
        );

        CvParsedEvent duplicate = new CvParsedEvent(
                UUID.randomUUID(),
                candidateId,
                parsedAt,
                original.name(), original.position(), original.posLabel(), original.email(),
                original.phone(), original.city(), original.telegram(), original.totalExp(),
                original.stack(), original.education(), original.verdict(), original.summary(),
                original.criteria(), original.experience(), original.questions()
        );

        kafkaTemplate.send(cvParsedTopic, candidateId, duplicate);

        TimeUnit.SECONDS.sleep(5);

        long count = candidateRepository.findAll().stream()
                .filter(c -> c.getId().equals(candidateId))
                .count();
        assertThat(count).isEqualTo(1);
    }
}
