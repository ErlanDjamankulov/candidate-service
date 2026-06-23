package kg.tunduk.cvscan.candidate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kg.tunduk.cvscan.candidate.model.Verdict;

import java.util.List;

public record CandidateWriteRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @NotBlank
        @Email
        String email,

        @Pattern(regexp = "^\\+\\d[\\d ]{6,20}$")
        String phone,

        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]+$")
        String position,

        String posLabel,

        String city,

        String telegram,

        String totalExp,

        String stack,

        String education,

        @NotNull
        Verdict verdict,

        String summary,

        @Valid
        List<CriteriaItemDto> criteria,

        @Valid
        List<ExperienceItemDto> experience,

        List<String> questions
) {

    public CandidateWriteRequest {
        if (criteria == null) {
            criteria = List.of();
        }
        if (experience == null) {
            experience = List.of();
        }
        if (questions == null) {
            questions = List.of();
        }
    }
}
