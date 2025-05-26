package com.ironlibrary.loan_service.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Clase para estructurar respuestas de errores de validación
 */
@Data
@AllArgsConstructor
public class ValidationErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> errors;
}
