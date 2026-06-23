package kg.tunduk.cvscan.candidate.dto;

import jakarta.validation.constraints.NotBlank;

public record ExperienceItemDto(
        @NotBlank String period,
        @NotBlank String company,
        @NotBlank String title,
        @NotBlank String duration
) {
}
