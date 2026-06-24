package kg.tunduk.cvscan.candidate.integration;

import kg.tunduk.cvscan.candidate.dto.CandidateWriteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@Sql(scripts = "/db/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CandidateApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void listCandidates_filterByVerdictAndStatus_returnsCorrectResults() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/v1/candidates?verdict=FIT&status=NEW"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
        for (Map<String, Object> candidate : content) {
            assertThat(candidate.get("verdict")).isEqualTo("FIT");
            assertThat(candidate.get("status")).isEqualTo("NEW");
        }
    }

    @Test
    void listCandidates_combinedFilters_withPaginationAndSearch() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/v1/candidates?position=java-middle&search=%D0%90&page=0&size=5"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("size")).isEqualTo(5);
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content.size()).isLessThanOrEqualTo(5);
    }

    @Test
    void getCandidate_existingId_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/v1/candidates/asanov-bakyt"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isEqualTo("asanov-bakyt");
    }

    @Test
    void getCandidate_nonExistingId_returns404WithCorrectErrorFormat() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/v1/candidates/no-such-candidate"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map body = response.getBody();
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("error")).isEqualTo("CANDIDATE_NOT_FOUND");
        assertThat(body.get("path")).isEqualTo("/api/v1/candidates/no-such-candidate");
        assertThat(body).containsKeys("message", "timestamp");
    }

    @Test
    void createCandidate_duplicateEmail_returns409() {
        CandidateWriteRequest request = new CandidateWriteRequest(
                "Новый Кандидат", "asanov.bakyt@email.com", "+996 700 000000",
                "java-middle", "Java — ведущий программист", "Бишкек", "@new_candidate",
                "~2 г.", "Java, Spring", "КНУ", kg.tunduk.cvscan.candidate.model.Verdict.PARTIAL,
                "summary", List.of(), List.of(), List.of());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/candidates"), new HttpEntity<>(request, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("error")).isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    void createCandidate_validRequest_returns201WithStatusNew() {
        CandidateWriteRequest request = new CandidateWriteRequest(
                "Тестов Тест Тестович", "tests.test@email.com", "+996 700 999888",
                "java-middle", "Java — ведущий программист", "Бишкек", "@test_candidate",
                "~1 г.", "Java, Spring Boot", "КНУ", kg.tunduk.cvscan.candidate.model.Verdict.PARTIAL,
                "summary", List.of(), List.of(), List.of());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/candidates"), new HttpEntity<>(request, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("NEW");
        assertThat(response.getBody().get("email")).isEqualTo("tests.test@email.com");
    }

    @Test
    void changeStatus_allowedTransition_savesHistoryAndReturns200() {
        StatusChangeBody body = new StatusChangeBody("IN_REVIEW", "Берём в работу");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/candidates/tilekov-aibek/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("IN_REVIEW");

        ResponseEntity<List> historyResponse = restTemplate.getForEntity(
                url("/api/v1/candidates/tilekov-aibek/status-history"), List.class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isNotEmpty();
        Map<String, Object> latest = (Map<String, Object>) historyResponse.getBody().get(0);
        assertThat(latest.get("toStatus")).isEqualTo("IN_REVIEW");
        assertThat(latest.get("fromStatus")).isEqualTo("NEW");
    }

    @Test
    void changeStatus_invalidTransition_returns422WithCorrectErrorCode() {
        StatusChangeBody body = new StatusChangeBody("REJECTED", null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/candidates/borbiev-almaz/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void deleteCandidate_existingId_returns204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/v1/candidates/kydyraliev-nurzat"),
                HttpMethod.DELETE,
                null,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
                url("/api/v1/candidates/kydyraliev-nurzat"), Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private record StatusChangeBody(String status, String comment) {
    }

}
