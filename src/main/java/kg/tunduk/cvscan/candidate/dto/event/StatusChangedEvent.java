package kg.tunduk.cvscan.candidate.dto.event;

import kg.tunduk.cvscan.candidate.model.CandidateStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StatusChangedEvent(
        UUID eventId,
        String candidateId,
        CandidateStatus fromStatus,
        CandidateStatus toStatus,
        OffsetDateTime changedAt
) {
}
