package com.ironlibrary.loan_service.exception;

/**
 * Excepción lanzada cuando no se encuentra un préstamo
 */
public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) {
        super(message);
    }
}
