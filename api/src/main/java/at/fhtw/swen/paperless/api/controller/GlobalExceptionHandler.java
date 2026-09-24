package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.controller.response.ErrorResponse;
import at.fhtw.swen.paperless.api.service.exception.TagAlreadyExistsException;
import at.fhtw.swen.paperless.api.service.exception.TagInUseException;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(
            TagNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("Tag not found: {} ({})",
                exception.getMessage(),
                request.getRequestURI());
        return build(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(TagAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTagAlreadyExists(
            TagAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        log.warn("Tag already exists: {} ({})",
                exception.getMessage(),
                request.getRequestURI());
        return build(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(TagInUseException.class)
    public ResponseEntity<ErrorResponse> handleTagInUse(
            TagInUseException exception,
            HttpServletRequest request
    ) {
        log.warn("Tag in use: {} ({})",
                exception.getMessage(),
                request.getRequestURI());
        return build(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                error -> error.getField(),
                                error -> error.getDefaultMessage(),
                                (first, second) -> first
                        ));
        log.warn("Validation failed for {}: {}",
                request.getRequestURI(),
                errors);
        ErrorResponse response =
                new ErrorResponse(
                        java.time.Instant.now(),
                        400,
                        "Bad Request",
                        "Validation failed",
                        request.getRequestURI(),
                        errors
                );
        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter format",
                request
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(status)
                .body(
                        new ErrorResponse(
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                request.getRequestURI()
                        )
                );
    }
}
