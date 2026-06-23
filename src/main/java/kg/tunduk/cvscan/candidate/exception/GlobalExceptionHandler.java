package kg.tunduk.cvscan.candidate.exception;

import jakarta.servlet.http.HttpServletRequest;
import kg.tunduk.cvscan.candidate.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Ошибка валидации входных данных",
                details,
                OffsetDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(CandidateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            CandidateNotFoundException ex, HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "CANDIDATE_NOT_FOUND",
                ex.getMessage(),
                null,
                OffsetDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_EMAIL",
                ex.getMessage(),
                null,
                OffsetDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "INVALID_STATUS_TRANSITION",
                ex.getMessage(),
                null,
                OffsetDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}
