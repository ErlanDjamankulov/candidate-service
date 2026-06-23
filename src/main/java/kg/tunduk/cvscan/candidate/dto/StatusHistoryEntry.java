package kg.tunduk.cvscan.candidate.dto;

import kg.tunduk.cvscan.candidate.model.CandidateStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StatusHistoryEntry(
        UUID id,
        String candidateId,
        CandidateStatus fromStatus,
        CandidateStatus toStatus,
        String comment,
        OffsetDateTime changedAt
) {
}
