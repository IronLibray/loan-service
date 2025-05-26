package com.ironlibrary.loan_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones de préstamos
 * Endpoints base: /api/loans
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;

    /**
     * GET /api/loans - Obtener todos los préstamos
     */
    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans() {
        log.info("Solicitud GET para obtener todos los préstamos");
        List<Loan> loans = loanService.findAllLoans();
        return ResponseEntity.ok(loans);
    }

    /**
     * GET /api/loans/{id} - Obtener préstamo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        log.info("Solicitud GET para obtener préstamo con ID: {}", id);
        Loan loan = loanService.findLoanById(id);
        return ResponseEntity.ok(loan);
    }

    /**
     * GET /api/loans/user/{userId} - Obtener préstamos por usuario
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Loan>> getLoansByUser(@PathVariable Long userId) {
        log.info("Solicitud GET para obtener préstamos del usuario: {}", userId);
        List<Loan> loans = loanService.findLoansByUser(userId);
        return ResponseEntity.ok(loans);
    }

    /**
     * GET /api/loans/user/{userId}/active - Obtener préstamos activos por usuario
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Loan>> getActiveLoansForUser(@PathVariable Long userId) {
        log.info("Solicitud GET para obtener préstamos activos del usuario: {}", userId);
        List<Loan> loans = loanService.findActiveLoansForUser(userId);
        return ResponseEntity.ok(loans);
    }

    /**
     * GET /api/loans/book/{bookId} - Obtener préstamos por libro
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<Loan>> getLoansByBook(@PathVariable Long bookId) {
        log.info("Solicitud GET para obtener préstamos del libro: {}", bookId);
        List<Loan> loans = loanService.findLoansByBook(bookId);
        return ResponseEntity.ok(loans);
    }

    /**
     * GET /api/loans/overdue - Obtener préstamos vencidos
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<Loan>> getOverdueLoans() {
        log.info("Solicitud GET para obtener préstamos vencidos");
        List<Loan> loans = loanService.findOverdueLoans();
        return ResponseEntity.ok(loans);
    }

    /**
     * GET /api/loans/due-soon?days=3 - Obtener préstamos que vencen pronto
     */
    @GetMapping("/due-soon")
    public ResponseEntity<List<Loan>> getLoansDueSoon(@RequestParam(defaultValue = "3") int days) {
        log.info("Solicitud GET para obtener préstamos que vencen en {} días", days);
        List<Loan> loans = loanService.findLoansDueSoon(days);
        return ResponseEntity.ok(loans);
    }

    /**
     * GET /api/loans/stats - Obtener estadísticas de préstamos
     */
    @GetMapping("/stats")
    public ResponseEntity<LoanService.LoanStatistics> getLoanStatistics() {
        log.info("Solicitud GET para obtener estadísticas de préstamos");
        LoanService.LoanStatistics stats = loanService.getLoanStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * POST /api/loans - Crear nuevo préstamo
     * Body: { "userId": 1, "bookId": 1, "notes": "Préstamo para estudio" }
     */
    @PostMapping
    public ResponseEntity<Loan> createLoan(@RequestBody CreateLoanRequest request) {
        log.info("Solicitud POST para crear préstamo - Usuario: {}, Libro: {}",
                request.getUserId(), request.getBookId());
        Loan loan = loanService.createLoan(request.getUserId(), request.getBookId(), request.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    /**
     * POST /api/loans/quick?userId=1&bookId=1 - Crear préstamo rápido
     */
    @PostMapping("/quick")
    public ResponseEntity<Loan> createQuickLoan(@RequestParam Long userId, @RequestParam Long bookId) {
        log.info("Solicitud POST para préstamo rápido - Usuario: {}, Libro: {}", userId, bookId);
        Loan loan = loanService.createLoan(userId, bookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    /**
     * PUT /api/loans/{id} - Actualizar préstamo
     */
    @PutMapping("/{id}")
    public ResponseEntity<Loan> updateLoan(@PathVariable Long id, @RequestBody Loan loan) {
        log.info("Solicitud PUT para actualizar préstamo con ID: {}", id);
        Loan updatedLoan = loanService.updateLoan(id, loan);
        return ResponseEntity.ok(updatedLoan);
    }

    /**
     * PATCH /api/loans/{id}/return - Devolver libro
     */
    @PatchMapping("/{id}/return")
    public ResponseEntity<Loan> returnBook(@PathVariable Long id) {
        log.info("Solicitud PATCH para devolver libro del préstamo ID: {}", id);
        Loan returnedLoan = loanService.returnBook(id);
        return ResponseEntity.ok(returnedLoan);
    }

    /**
     * PATCH /api/loans/{id}/extend?days=7 - Extender préstamo
     */
    @PatchMapping("/{id}/extend")
    public ResponseEntity<Loan> extendLoan(@PathVariable Long id, @RequestParam int days) {
        log.info("Solicitud PATCH para extender préstamo ID: {} por {} días", id, days);
        Loan extendedLoan = loanService.extendLoan(id, days);
        return ResponseEntity.ok(extendedLoan);
    }

    /**
     * DELETE /api/loans/{id} - Eliminar préstamo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        log.info("Solicitud DELETE para eliminar préstamo con ID: {}", id);
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint de health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Loan Service is running on port 8083");
    }

    /**
     * DTO para crear préstamos
     */
    public static class CreateLoanRequest {
        private Long userId;
        private Long bookId;
        private String notes;

        // Constructors
        public CreateLoanRequest() {}

        public CreateLoanRequest(Long userId, Long bookId, String notes) {
            this.userId = userId;
            this.bookId = bookId;
            this.notes = notes;
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
