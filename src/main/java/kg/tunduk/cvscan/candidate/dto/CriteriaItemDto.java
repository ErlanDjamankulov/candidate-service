package kg.tunduk.cvscan.candidate.dto;

import jakarta.validation.constraints.NotBlank;

public record CriteriaItemDto(
        @NotBlank String key,
        @NotBlank String result,
        @NotBlank String comment
) {
}
