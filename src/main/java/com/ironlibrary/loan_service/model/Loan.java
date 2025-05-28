package com.ironlibrary.loan_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Modelo Loan - CORREGIDO para funcionar con H2
 * Cambios principales:
 * - @Enumerated(EnumType.STRING) compatible con H2
 * - Métodos calculados con @JsonIgnore
 * - Manejo seguro de nulls
 */
@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    // IMPORTANTE: Para H2, usar STRING en lugar de ORDINAL
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Verifica si el préstamo está vencido
     * @return true si la fecha de vencimiento ya pasó y no se ha devuelto
     */
    @JsonIgnore // Excluir de serialización JSON
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE &&
                dueDate != null &&
                LocalDate.now().isAfter(dueDate);
    }

    /**
     * Calcula los días de retraso
     * @return número de días de retraso, 0 si no está vencido o hay datos nulos
     */
    @JsonIgnore // Excluir de serialización JSON
    public long getDaysOverdue() {
        if (!isOverdue() || dueDate == null) {
            return 0;
        }
        return LocalDate.now().toEpochDay() - dueDate.toEpochDay();
    }

    /**
     * Calcula la duración del préstamo en días
     * @return número de días entre fechas, 0 si hay nulls
     */
    @JsonIgnore // Excluir de serialización JSON
    public long getLoanDurationDays() {
        if (dueDate == null || loanDate == null) {
            return 0; // Retornar 0 en lugar de lanzar excepción
        }
        return dueDate.toEpochDay() - loanDate.toEpochDay();
    }

    /**
     * Verifica si el préstamo puede ser devuelto
     * @return true si está activo o vencido
     */
    @JsonIgnore // Excluir de serialización JSON
    public boolean canBeReturned() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.OVERDUE;
    }

    /**
     * Constructor para crear un nuevo préstamo
     */
    public Loan(Long userId, Long bookId, LocalDate loanDate, LocalDate dueDate) {
        this.userId = userId;
        this.bookId = bookId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.status = LoanStatus.ACTIVE;
    }

    /**
     * Constructor con notas
     */
    public Loan(Long userId, Long bookId, LocalDate loanDate, LocalDate dueDate, String notes) {
        this(userId, bookId, loanDate, dueDate);
        this.notes = notes;
    }
}
