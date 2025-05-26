package com.ironlibrary.loan_service.service;

import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la lógica de negocio de préstamos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * Crear un nuevo préstamo
     */
    public Loan createLoan(Long userId, Long bookId) {
        return createLoan(userId, bookId, null);
    }

    /**
     * Crear un nuevo préstamo con notas
     */
    public Loan createLoan(Long userId, Long bookId, String notes) {
        log.info("Creando préstamo - Usuario: {}, Libro: {}", userId, bookId);

        // 1. Validar usuario
        UserDto user = validateUser(userId);

        // 2. Validar libro
        BookDto book = validateBook(bookId);

        // 3. Verificar límites del usuario
        validateUserLimits(userId, user);

        // 4. Verificar que el usuario no tenga ya este libro prestado
        if (loanRepository.hasActiveLoanForBook(userId, bookId)) {
            throw new IllegalArgumentException("El usuario ya tiene este libro prestado");
        }

        // 5. Crear el préstamo
        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(user.getLoanDurationDays());

        Loan loan = new Loan(userId, bookId, loanDate, dueDate, notes);

        // 6. Actualizar disponibilidad del libro
        try {
            bookServiceClient.updateAvailability(bookId, -1);
            log.info("Disponibilidad del libro {} actualizada", bookId);
        } catch (Exception e) {
            log.error("Error actualizando disponibilidad del libro {}: {}", bookId, e.getMessage());
            throw new BookNotAvailableException("Error al actualizar la disponibilidad del libro");
        }

        // 7. Guardar préstamo
        Loan savedLoan = loanRepository.save(loan);
        log.info("Préstamo creado exitosamente con ID: {}", savedLoan.getId());

        return savedLoan;
    }

    /**
     * Obtener todos los préstamos
     */
    @Transactional(readOnly = true)
    public List<Loan> findAllLoans() {
        log.info("Obteniendo todos los préstamos");
        return loanRepository.findAll();
    }

    /**
     * Buscar préstamo por ID
     */
    @Transactional(readOnly = true)
    public Loan findLoanById(Long id) {
        log.info("Buscando préstamo con ID: {}", id);
        return loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException("Préstamo no encontrado con ID: " + id));
    }

    /**
     * Buscar préstamos por usuario
     */
    @Transactional(readOnly = true)
    public List<Loan> findLoansByUser(Long userId) {
        log.info("Buscando préstamos del usuario: {}", userId);

        // Verificar que el usuario existe
        validateUser(userId);

        return loanRepository.findByUserId(userId);
    }

    /**
     * Buscar préstamos activos por usuario
     */
    @Transactional(readOnly = true)
    public List<Loan> findActiveLoansForUser(Long userId) {
        log.info("Buscando préstamos activos del usuario: {}", userId);
        return loanRepository.findByUserIdAndStatus(userId, LoanStatus.ACTIVE);
    }

    /**
     * Buscar préstamos por libro
     */
    @Transactional(readOnly = true)
    public List<Loan> findLoansByBook(Long bookId) {
        log.info("Buscando préstamos del libro: {}", bookId);

        // Verificar que el libro existe
        validateBook(bookId);

        return loanRepository.findByBookId(bookId);
    }

    /**
     * Devolver un libro
     */
    public Loan returnBook(Long loanId) {
        log.info("Procesando devolución del préstamo ID: {}", loanId);

        Loan loan = findLoanById(loanId);

        // Verificar que el préstamo puede ser devuelto
        if (!loan.canBeReturned()) {
            throw new LoanAlreadyReturnedException("El préstamo ya fue devuelto o cancelado");
        }

        // Actualizar el préstamo
        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);

        // Actualizar disponibilidad del libro
        try {
            bookServiceClient.updateAvailability(loan.getBookId(), 1);
            log.info("Disponibilidad del libro {} actualizada en devolución", loan.getBookId());
        } catch (Exception e) {
            log.error("Error actualizando disponibilidad en devolución del libro {}: {}",
                    loan.getBookId(), e.getMessage());
            // No falla la devolución, pero se registra el error
        }

        Loan returnedLoan = loanRepository.save(loan);
        log.info("Libro devuelto exitosamente");

        return returnedLoan;
    }

    /**
     * Actualizar préstamo
     */
    public Loan updateLoan(Long id, Loan loanUpdate) {
        log.info("Actualizando préstamo con ID: {}", id);
        Loan existingLoan = findLoanById(id);

        // Solo se pueden actualizar ciertos campos
        existingLoan.setDueDate(loanUpdate.getDueDate());
        existingLoan.setNotes(loanUpdate.getNotes());

        Loan updatedLoan = loanRepository.save(existingLoan);
        log.info("Préstamo actualizado exitosamente");
        return updatedLoan;
    }

    /**
     * Eliminar préstamo (solo si no está activo)
     */
    public void deleteLoan(Long id) {
        log.info("Eliminando préstamo con ID: {}", id);
        Loan loan = findLoanById(id);

        if (loan.getStatus() == LoanStatus.ACTIVE) {
            throw new IllegalArgumentException("No se puede eliminar un préstamo activo. Debe devolverse primero.");
        }

        loanRepository.delete(loan);
        log.info("Préstamo eliminado exitosamente");
    }

    /**
     * Obtener préstamos vencidos
     */
    @Transactional(readOnly = true)
    public List<Loan> findOverdueLoans() {
        log.info("Obteniendo préstamos vencidos");
        List<Loan> overdueLoans = loanRepository.findOverdueLoans();

        // Actualizar estado a OVERDUE
        overdueLoans.forEach(loan -> {
            if (loan.getStatus() == LoanStatus.ACTIVE) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }
        });

        return overdueLoans;
    }

    /**
     * Obtener préstamos que vencen pronto
     */
    @Transactional(readOnly = true)
    public List<Loan> findLoansDueSoon(int days) {
        log.info("Obteniendo préstamos que vencen en {} días", days);
        LocalDate endDate = LocalDate.now().plusDays(days);
        return loanRepository.findLoansDueSoon(endDate);
    }

    /**
     * Extender fecha de vencimiento
     */
    public Loan extendLoan(Long loanId, int days) {
        log.info("Extendiendo préstamo ID: {} por {} días", loanId, days);
        Loan loan = findLoanById(loanId);

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalArgumentException("Solo se pueden extender préstamos activos");
        }

        if (days <= 0 || days > 30) {
            throw new IllegalArgumentException("Los días de extensión deben estar entre 1 y 30");
        }

        loan.setDueDate(loan.getDueDate().plusDays(days));

        Loan extendedLoan = loanRepository.save(loan);
        log.info("Préstamo extendido exitosamente hasta: {}", extendedLoan.getDueDate());

        return extendedLoan;
    }

    /**
     * Obtener estadísticas de préstamos
     */
    @Transactional(readOnly = true)
    public LoanStatistics getLoanStatistics() {
        log.info("Obteniendo estadísticas de préstamos");

        List<Object[]> stats = loanRepository.getLoanStatistics();
        Map<LoanStatus, Long> statusCounts = stats.stream()
                .collect(Collectors.toMap(
                        row -> (LoanStatus) row[0],
                        row -> (Long) row[1]
                ));

        long totalLoans = loanRepository.count();
        long activeLoans = statusCounts.getOrDefault(LoanStatus.ACTIVE, 0L);
        long overdueLoans = statusCounts.getOrDefault(LoanStatus.OVERDUE, 0L);
        long returnedLoans = statusCounts.getOrDefault(LoanStatus.RETURNED, 0L);

        return new LoanStatistics(totalLoans, activeLoans, overdueLoans, returnedLoans);
    }

    /**
     * Validar usuario y obtener datos
     */
    private UserDto validateUser(Long userId) {
        try {
            UserDto user = userServiceClient.getUserById(userId);

            if (!userServiceClient.validateUser(userId)) {
                throw new UserNotValidException("El usuario no puede pedir libros prestados");
            }

            return user;
        } catch (Exception e) {
            log.error("Error validando usuario {}: {}", userId, e.getMessage());
            throw new UserNotValidException("Usuario no válido o no encontrado: " + userId);
        }
    }

    /**
     * Validar libro y obtener datos
     */
    private BookDto validateBook(Long bookId) {
        try {
            BookDto book = bookServiceClient.getBookById(bookId);

            if (!bookServiceClient.isBookAvailable(bookId)) {
                throw new BookNotAvailableException("El libro no está disponible para préstamo");
            }

            return book;
        } catch (Exception e) {
            log.error("Error validando libro {}: {}", bookId, e.getMessage());
            throw new BookNotAvailableException("Libro no válido o no disponible: " + bookId);
        }
    }

    /**
     * Validar límites del usuario
     */
    private void validateUserLimits(Long userId, UserDto user) {
        long activeLoans = loanRepository.countActiveLoansForUser(userId);
        int maxAllowed = user.getMaxBooksAllowed();

        if (activeLoans >= maxAllowed) {
            throw new UserNotValidException(
                    String.format("El usuario ha alcanzado el límite de libros prestados (%d/%d)",
                            activeLoans, maxAllowed));
        }
    }

    /**
     * Clase interna para estadísticas
     */
    public static class LoanStatistics {
        public final long totalLoans;
        public final long activeLoans;
        public final long overdueLoans;
        public final long returnedLoans;

        public LoanStatistics(long totalLoans, long activeLoans, long overdueLoans, long returnedLoans) {
            this.totalLoans = totalLoans;
            this.activeLoans = activeLoans;
            this.overdueLoans = overdueLoans;
            this.returnedLoans = returnedLoans;
        }
    }
}
