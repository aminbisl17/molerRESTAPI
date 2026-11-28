package com.example.moler.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // SQL Server error codes
    private static final int SQL_UNIQUE_CONSTRAINT = 2627;
    private static final int SQL_UNIQUE_INDEX = 2601;
    private static final int SQL_FOREIGN_KEY_OR_CHECK = 547;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "message", ex.getMessage()
                ));
    }

    // Ownership / permission checks inside services
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleForbidden(AccessDeniedException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "status", 403,
                        "message", ex.getMessage()
                ));
    }

    // Database constraint violations that got past the service checks
    // (e.g. two concurrent requests) are returned as 400 instead of 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "message", constraintMessage(ex)
                ));
    }

    private String constraintMessage(DataIntegrityViolationException ex) {

        String text = ex.getMostSpecificCause().getMessage();
        text = text == null ? "" : text;

        int errorCode = sqlErrorCode(ex);

        if (errorCode == SQL_UNIQUE_CONSTRAINT || errorCode == SQL_UNIQUE_INDEX) {

            if (text.contains("UQ_klienti_unique_details")) {
                return "Ky klient me keto te dhena ekziston tashmë.";
            }

            // The other unique constraint on klienti is the phone number
            if (text.contains(".klienti'")) {
                return "Ky numër telefoni ekziston tashmë.";
            }

            return "Ky rekord ekziston tashmë.";
        }

        if (errorCode == SQL_FOREIGN_KEY_OR_CHECK) {

            if (text.contains("klienti")) {
                return "Klienti ka projekte te lidhura ose nuk ekziston.";
            }

            return "Veprimi nuk lejohet sepse rekordi eshte i lidhur me te dhena te tjera.";
        }

        return "Te dhenat nuk jane te vlefshme.";
    }

    private int sqlErrorCode(Throwable ex) {

        Throwable current = ex;

        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException.getErrorCode();
            }
            current = current.getCause();
        }

        return -1;
    }
}
