package com.ironlibrary.loan_service.exception;

/**
 * Excepción lanzada cuando un usuario no es válido para préstamos
 */
public class UserNotValidException extends RuntimeException {
    public UserNotValidException(String message) {
        super(message);
    }
}
