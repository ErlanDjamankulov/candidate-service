package kg.tunduk.cvscan.candidate.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kg.tunduk.cvscan.candidate.dto.CriteriaItemDto;
import kg.tunduk.cvscan.candidate.dto.ExperienceItemDto;
import kg.tunduk.cvscan.candidate.model.Verdict;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CvParsedEvent(
        UUID eventId,
        String candidateId,
        OffsetDateTime parsedAt,
        String name,
        String position,
        String posLabel,
        String email,
        String phone,
        String city,
        String telegram,
        String totalExp,
        String stack,
        String education,
        Verdict verdict,
        String summary,
        List<CriteriaItemDto> criteria,
        List<ExperienceItemDto> experience,
        List<String> questions
) {
}
