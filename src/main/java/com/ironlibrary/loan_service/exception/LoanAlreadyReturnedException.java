package com.ironlibrary.loan_service.exception;

/**
 * Excepción lanzada cuando se intenta devolver un préstamo ya devuelto
 */
public class LoanAlreadyReturnedException extends RuntimeException {
    public LoanAlreadyReturnedException(String message) {
        super(message);
    }
}
