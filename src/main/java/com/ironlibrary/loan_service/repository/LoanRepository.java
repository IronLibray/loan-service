package com.ironlibrary.loan_service.repository;


import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Loan
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /**
     * Buscar préstamos por usuario
     */
    List<Loan> findByUserId(Long userId);

    /**
     * Buscar préstamos por libro
     */
    List<Loan> findByBookId(Long bookId);

    /**
     * Buscar préstamos por estado
     */
    List<Loan> findByStatus(LoanStatus status);

    /**
     * Buscar préstamos activos por usuario
     */
    List<Loan> findByUserIdAndStatus(Long userId, LoanStatus status);

    /**
     * Buscar préstamos activos por libro
     */
    List<Loan> findByBookIdAndStatus(Long bookId, LoanStatus status);

    /**
     * Buscar préstamos vencidos
     */
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < CURRENT_DATE")
    List<Loan> findOverdueLoans();

    /**
     * Buscar préstamos por rango de fechas
     */
    List<Loan> findByLoanDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Buscar préstamos que vencen en una fecha específica
     */
    List<Loan> findByDueDateAndStatus(LocalDate dueDate, LoanStatus status);

    /**
     * Contar préstamos activos por usuario
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.userId = :userId AND l.status = 'ACTIVE'")
    Long countActiveLoansForUser(@Param("userId") Long userId);

    /**
     * Contar préstamos activos por libro
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.bookId = :bookId AND l.status = 'ACTIVE'")
    Long countActiveLoansForBook(@Param("bookId") Long bookId);

    /**
     * Buscar préstamos por usuario y estado
     */
    @Query("SELECT l FROM Loan l WHERE l.userId = :userId AND l.status IN :statuses")
    List<Loan> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<LoanStatus> statuses);

    /**
     * Buscar préstamos que vencen pronto (próximos N días)
     */
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate BETWEEN CURRENT_DATE AND :endDate")
    List<Loan> findLoansDueSoon(@Param("endDate") LocalDate endDate);

    /**
     * Obtener estadísticas de préstamos
     */
    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> getLoanStatistics();

    /**
     * Verificar si usuario tiene préstamo activo del libro
     */
    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.userId = :userId AND l.bookId = :bookId AND l.status = 'ACTIVE'")
    boolean hasActiveLoanForBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
