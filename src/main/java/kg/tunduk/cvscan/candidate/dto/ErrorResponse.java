package kg.tunduk.cvscan.candidate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<FieldError> details,
        OffsetDateTime timestamp,
        String path
) {
    public record FieldError(String field, String message) {
    }
}
