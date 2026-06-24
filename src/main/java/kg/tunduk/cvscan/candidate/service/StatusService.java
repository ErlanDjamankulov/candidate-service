package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.exception.InvalidStatusTransitionException;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.StatusHistory;
import kg.tunduk.cvscan.candidate.repository.StatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Машина состояний кандидата.
 *
 * Допустимые переходы:
 * NEW -> IN_REVIEW
 * IN_REVIEW -> INVITED
 * IN_REVIEW -> REJECTED
 * INVITED -> APPROVED
 *
 * INVITED -> REJECTED
 */
@Service
public class StatusService {

    private static final Map<CandidateStatus, Set<CandidateStatus>> ALLOWED_TRANSITIONS = Map.of(
            CandidateStatus.NEW, Set.of(CandidateStatus.IN_REVIEW),
            CandidateStatus.IN_REVIEW, Set.of(CandidateStatus.INVITED, CandidateStatus.REJECTED),
            CandidateStatus.INVITED, Set.of(CandidateStatus.APPROVED, CandidateStatus.REJECTED),
            CandidateStatus.APPROVED, Set.of(),
            CandidateStatus.REJECTED, Set.of()
    );

    private final StatusHistoryRepository statusHistoryRepository;

    public StatusService(StatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public boolean isTransitionAllowed(CandidateStatus from, CandidateStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void assertTransitionAllowed(CandidateStatus from, CandidateStatus to) {
        if (!isTransitionAllowed(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    public StatusHistory recordTransition(String candidateId, CandidateStatus from, CandidateStatus to, String comment) {
        StatusHistory entry = StatusHistory.builder()
                .candidateId(candidateId)
                .fromStatus(from)
                .toStatus(to)
                .comment(comment)
                .changedAt(OffsetDateTime.now())
                .build();
        return statusHistoryRepository.save(entry);
    }

    public List<StatusHistory> getHistory(String candidateId) {
        return statusHistoryRepository.findByCandidateIdOrderByChangedAtDesc(candidateId);
    }
}
