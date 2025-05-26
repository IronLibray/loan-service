package com.ironlibrary.loan_service.model;


/**
 * Enum para los estados de préstamos
 */
public enum LoanStatus {
    ACTIVE("Activo", "El libro está prestado"),
    RETURNED("Devuelto", "El libro ha sido devuelto"),
    OVERDUE("Vencido", "El préstamo está vencido"),
    CANCELLED("Cancelado", "El préstamo fue cancelado");

    private final String displayName;
    private final String description;

    LoanStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Verifica si el estado permite devolución
     */
    public boolean allowsReturn() {
        return this == ACTIVE || this == OVERDUE;
    }

    /**
     * Verifica si el préstamo está completado
     */
    public boolean isCompleted() {
        return this == RETURNED || this == CANCELLED;
    }
}
