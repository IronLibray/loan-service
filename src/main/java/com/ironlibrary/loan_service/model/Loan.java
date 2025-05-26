package com.ironlibrary.loan_service.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Modelo Loan - Representa un préstamo en el sistema
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(length = 500)
    private String notes;

    /**
     * Verifica si el préstamo está vencido
     * @return true si la fecha de vencimiento ya pasó y no se ha devuelto
     */
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE &&
                LocalDate.now().isAfter(dueDate);
    }

    /**
     * Calcula los días de retraso
     * @return número de días de retraso, 0 si no está vencido
     */
    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return LocalDate.now().toEpochDay() - dueDate.toEpochDay();
    }

    /**
     * Calcula la duración del préstamo en días
     * @return número de días entre la fecha de préstamo y vencimiento
     */
    public long getLoanDurationDays() {
        return dueDate.toEpochDay() - loanDate.toEpochDay();
    }

    /**
     * Verifica si el préstamo puede ser devuelto
     * @return true si está activo o vencido
     */
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
