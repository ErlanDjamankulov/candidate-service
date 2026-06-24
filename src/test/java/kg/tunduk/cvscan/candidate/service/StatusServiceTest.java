package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.exception.InvalidStatusTransitionException;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.StatusHistory;
import kg.tunduk.cvscan.candidate.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new StatusService(statusHistoryRepository);
    }

    @ParameterizedTest(name = "{0} -> {1} допустим")
    @CsvSource({
            "NEW, IN_REVIEW",
            "IN_REVIEW, INVITED",
            "IN_REVIEW, REJECTED",
            "INVITED, APPROVED",
            "INVITED, REJECTED"
    })
    @DisplayName("Допустимые переходы возвращают true и не бросают исключение")
    void allowedTransitions_areAccepted(CandidateStatus from, CandidateStatus to) {
        assertThat(statusService.isTransitionAllowed(from, to)).isTrue();
        assertThatCode(() -> statusService.assertTransitionAllowed(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1} недопустим")
    @CsvSource({
            "NEW, INVITED",
            "NEW, APPROVED",
            "NEW, REJECTED",
            "IN_REVIEW, NEW",
            "IN_REVIEW, APPROVED",
            "INVITED, NEW",
            "INVITED, IN_REVIEW",
            "APPROVED, REJECTED",
            "APPROVED, NEW",
            "REJECTED, NEW",
            "REJECTED, IN_REVIEW"
    })
    @DisplayName("Недопустимые переходы отклоняются")
    void invalidTransitions_areRejected(CandidateStatus from, CandidateStatus to) {
        assertThat(statusService.isTransitionAllowed(from, to)).isFalse();
        assertThatThrownBy(() -> statusService.assertTransitionAllowed(from, to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    @Test
    @DisplayName("Терминальные статусы APPROVED и REJECTED не имеют допустимых переходов")
    void terminalStatuses_haveNoAllowedTransitions() {
        for (CandidateStatus target : CandidateStatus.values()) {
            assertThat(statusService.isTransitionAllowed(CandidateStatus.APPROVED, target)).isFalse();
            assertThat(statusService.isTransitionAllowed(CandidateStatus.REJECTED, target)).isFalse();
        }
    }

    @Test
    @DisplayName("recordTransition сохраняет переход с корректными from/to/comment")
    void recordTransition_savesHistoryEntryWithCorrectFields() {
        String candidateId = "asanov-bakyt";
        when(statusHistoryRepository.save(any(StatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatusHistory saved = statusService.recordTransition(
                candidateId, CandidateStatus.NEW, CandidateStatus.IN_REVIEW, "Берём в работу");

        ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());

        StatusHistory captured = captor.getValue();
        assertThat(captured.getCandidateId()).isEqualTo(candidateId);
        assertThat(captured.getFromStatus()).isEqualTo(CandidateStatus.NEW);
        assertThat(captured.getToStatus()).isEqualTo(CandidateStatus.IN_REVIEW);
        assertThat(captured.getComment()).isEqualTo("Берём в работу");
        assertThat(captured.getChangedAt()).isNotNull();
        assertThat(saved).isEqualTo(captured);
    }
}
