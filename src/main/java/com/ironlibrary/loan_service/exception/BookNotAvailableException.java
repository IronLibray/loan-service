package com.ironlibrary.loan_service.exception;

/**
 * Excepción lanzada cuando un libro no está disponible para préstamo
 */
public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}
