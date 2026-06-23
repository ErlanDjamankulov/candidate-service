package kg.tunduk.cvscan.candidate.dto;

import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;

import java.time.OffsetDateTime;
import java.util.List;


public record CandidateResponse(
        String id,
        String name,
        String email,
        String phone,
        String position,
        String posLabel,
        String city,
        String telegram,
        String totalExp,
        String stack,
        String education,
        Verdict verdict,
        String summary,
        List<CriteriaItemDto> criteria,
        List<ExperienceItemDto> experience,
        List<String> questions,
        CandidateStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
