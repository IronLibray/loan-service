package com.ironlibrary.loan_service.repository;

import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class LoanRepositoryIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    private Loan activeLoan;
    private Loan overdueLoan;
    private Loan returnedLoan;

    @BeforeEach
    void setUp() {
        System.out.println("🧪 === Repository Integration Test FINAL - Setup ===");

        loanRepository.deleteAll();

        setupTestData();

        System.out.println("✅ Datos de prueba creados: " + loanRepository.count() + " préstamos");
    }

    private void setupTestData() {
        // Préstamo activo normal
        activeLoan = new Loan();
        activeLoan.setUserId(1L);
        activeLoan.setBookId(101L);
        activeLoan.setLoanDate(LocalDate.now().minusDays(5));
        activeLoan.setDueDate(LocalDate.now().plusDays(10));
        activeLoan.setStatus(LoanStatus.ACTIVE);
        activeLoan.setNotes("Préstamo activo normal");

        // Préstamo vencido
        overdueLoan = new Loan();
        overdueLoan.setUserId(1L);  // Mismo usuario
        overdueLoan.setBookId(102L); // Diferente libro
        overdueLoan.setLoanDate(LocalDate.now().minusDays(25));
        overdueLoan.setDueDate(LocalDate.now().minusDays(5)); // Vencido hace 5 días
        overdueLoan.setStatus(LoanStatus.ACTIVE); // Todavía marcado como activo
        overdueLoan.setNotes("Préstamo vencido");

        // Préstamo devuelto
        returnedLoan = new Loan();
        returnedLoan.setUserId(2L); // Diferente usuario
        returnedLoan.setBookId(103L);
        returnedLoan.setLoanDate(LocalDate.now().minusDays(30));
        returnedLoan.setDueDate(LocalDate.now().minusDays(16));
        returnedLoan.setReturnDate(LocalDate.now().minusDays(2));
        returnedLoan.setStatus(LoanStatus.RETURNED);
        returnedLoan.setNotes("Préstamo ya devuelto");

        // Guardar todos en la base de datos
        activeLoan = loanRepository.save(activeLoan);
        overdueLoan = loanRepository.save(overdueLoan);
        returnedLoan = loanRepository.save(returnedLoan);
    }

    @Test
    void repositoryBasics_ShouldWork() {
        System.out.println("🧪 Repository Test: Operaciones básicas");

        // Verificar que se guardaron correctamente
        assertEquals(3, loanRepository.count());

        // Test findById
        Optional<Loan> found = loanRepository.findById(activeLoan.getId());
        assertTrue(found.isPresent());
        assertEquals(activeLoan.getUserId(), found.get().getUserId());
        assertEquals(activeLoan.getBookId(), found.get().getBookId());

        // Test findAll
        List<Loan> allLoans = loanRepository.findAll();
        assertEquals(3, allLoans.size());

        System.out.println("✅ Operaciones básicas funcionan correctamente");
    }

    @Test
    void findByUserId_ShouldReturnUserLoans() {
        System.out.println("🧪 Repository Test: Buscar préstamos por usuario");

        // Usuario 1 tiene 2 préstamos (activo y vencido)
        List<Loan> user1Loans = loanRepository.findByUserId(1L);
        assertEquals(2, user1Loans.size());
        assertTrue(user1Loans.stream().allMatch(loan -> loan.getUserId().equals(1L)));

        // Usuario 2 tiene 1 préstamo (devuelto)
        List<Loan> user2Loans = loanRepository.findByUserId(2L);
        assertEquals(1, user2Loans.size());
        assertEquals(2L, user2Loans.get(0).getUserId());

        // Usuario inexistente
        List<Loan> noUserLoans = loanRepository.findByUserId(999L);
        assertTrue(noUserLoans.isEmpty());

        System.out.println("✅ Búsqueda por usuario funciona correctamente");
    }

    @Test
    void findByBookId_ShouldReturnBookLoans() {
        System.out.println("🧪 Repository Test: Buscar préstamos por libro");

        // Cada libro tiene exactamente 1 préstamo
        List<Loan> book101Loans = loanRepository.findByBookId(101L);
        assertEquals(1, book101Loans.size());
        assertEquals(activeLoan.getId(), book101Loans.get(0).getId());

        List<Loan> book102Loans = loanRepository.findByBookId(102L);
        assertEquals(1, book102Loans.size());
        assertEquals(overdueLoan.getId(), book102Loans.get(0).getId());

        List<Loan> book103Loans = loanRepository.findByBookId(103L);
        assertEquals(1, book103Loans.size());
        assertEquals(returnedLoan.getId(), book103Loans.get(0).getId());

        System.out.println("✅ Búsqueda por libro funciona correctamente");
    }

    @Test
    void findByStatus_ShouldFilterByStatus() {
        System.out.println("🧪 Repository Test: Filtrar por estado");

        // Préstamos activos (incluyendo vencidos que aún están ACTIVE)
        List<Loan> activeLoans = loanRepository.findByStatus(LoanStatus.ACTIVE);
        assertEquals(2, activeLoans.size());

        // Préstamos devueltos
        List<Loan> returnedLoans = loanRepository.findByStatus(LoanStatus.RETURNED);
        assertEquals(1, returnedLoans.size());
        assertEquals(returnedLoan.getId(), returnedLoans.get(0).getId());

        // Estado que no existe
        List<Loan> cancelledLoans = loanRepository.findByStatus(LoanStatus.CANCELLED);
        assertTrue(cancelledLoans.isEmpty());

        System.out.println("✅ Filtrado por estado funciona correctamente");
    }

    @Test
    void findOverdueLoans_ShouldUseCustomQuery() {
        System.out.println("🧪 Repository Test: Query personalizada para préstamos vencidos");

        // Solo el overdueLoan debería aparecer (ACTIVE + fecha vencida)
        List<Loan> overdueLoans = loanRepository.findOverdueLoans();
        assertEquals(1, overdueLoans.size());

        Loan foundOverdue = overdueLoans.get(0);
        assertEquals(overdueLoan.getId(), foundOverdue.getId());
        assertEquals(LoanStatus.ACTIVE, foundOverdue.getStatus());
        assertTrue(foundOverdue.getDueDate().isBefore(LocalDate.now()));

        System.out.println("✅ Query de préstamos vencidos funciona correctamente");
    }

    @Test
    void countActiveLoansForUser_ShouldReturnCorrectCount() {
        System.out.println("🧪 Repository Test: Contar préstamos activos por usuario");

        // Usuario 1: 2 préstamos activos
        Long user1Count = loanRepository.countActiveLoansForUser(1L);
        assertEquals(2L, user1Count);

        // Usuario 2: 0 préstamos activos (tiene 1 devuelto)
        Long user2Count = loanRepository.countActiveLoansForUser(2L);
        assertEquals(0L, user2Count);

        // Usuario inexistente: 0
        Long noUserCount = loanRepository.countActiveLoansForUser(999L);
        assertEquals(0L, noUserCount);

        System.out.println("✅ Conteo de préstamos activos funciona correctamente");
    }

    @Test
    void hasActiveLoanForBook_ShouldDetectActiveLoans() {
        System.out.println("🧪 Repository Test: Detectar préstamo activo para libro específico");

        // Usuario 1 tiene préstamo activo del libro 101
        assertTrue(loanRepository.hasActiveLoanForBook(1L, 101L));

        // Usuario 1 tiene préstamo activo del libro 102 (aunque vencido)
        assertTrue(loanRepository.hasActiveLoanForBook(1L, 102L));

        // Usuario 2 NO tiene préstamo activo del libro 103 (está devuelto)
        assertFalse(loanRepository.hasActiveLoanForBook(2L, 103L));

        // Usuario 1 NO tiene préstamo del libro 103
        assertFalse(loanRepository.hasActiveLoanForBook(1L, 103L));

        System.out.println("✅ Detección de préstamos activos funciona correctamente");
    }

    @Test
    void findLoansDueSoon_ShouldFindUpcomingDueDates() {
        System.out.println("🧪 Repository Test: Préstamos que vencen pronto");

        // Crear préstamo que vence en 2 días
        Loan dueSoonLoan = new Loan();
        dueSoonLoan.setUserId(3L);
        dueSoonLoan.setBookId(201L);
        dueSoonLoan.setLoanDate(LocalDate.now().minusDays(12));
        dueSoonLoan.setDueDate(LocalDate.now().plusDays(2)); // Vence en 2 días
        dueSoonLoan.setStatus(LoanStatus.ACTIVE);
        dueSoonLoan.setNotes("Vence pronto");
        loanRepository.save(dueSoonLoan);

        // Buscar préstamos que vencen en los próximos 5 días
        LocalDate endDate = LocalDate.now().plusDays(5);
        List<Loan> dueSoonLoans = loanRepository.findLoansDueSoon(endDate);

        // Debería encontrar:
        // 1. activeLoan (vence en 10 días) -> NO
        // 2. dueSoonLoan (vence en 2 días) -> SÍ
        assertEquals(1, dueSoonLoans.size());
        assertEquals(dueSoonLoan.getId(), dueSoonLoans.get(0).getId());

        // Buscar en ventana más amplia (15 días)
        LocalDate widerEndDate = LocalDate.now().plusDays(15);
        List<Loan> widerSearch = loanRepository.findLoansDueSoon(widerEndDate);
        assertEquals(2, widerSearch.size());

        System.out.println("✅ Búsqueda de vencimientos próximos funciona correctamente");
    }

    @Test
    void getLoanStatistics_ShouldGroupByStatus() {
        System.out.println("🧪 Repository Test: Estadísticas agrupadas por estado");

        List<Object[]> stats = loanRepository.getLoanStatistics();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // Convertir a mapa para verificar fácilmente
        boolean foundActive = false;
        boolean foundReturned = false;
        long activeCount = 0;
        long returnedCount = 0;

        for (Object[] stat : stats) {
            LoanStatus status = (LoanStatus) stat[0];
            Long count = (Long) stat[1];

            if (status == LoanStatus.ACTIVE) {
                activeCount = count;
                foundActive = true;
            } else if (status == LoanStatus.RETURNED) {
                returnedCount = count;
                foundReturned = true;
            }
        }

        assertTrue(foundActive, "Debería encontrar estadísticas de préstamos activos");
        assertTrue(foundReturned, "Debería encontrar estadísticas de préstamos devueltos");
        assertEquals(2L, activeCount, "Debería haber 2 préstamos activos");
        assertEquals(1L, returnedCount, "Debería haber 1 préstamo devuelto");

        System.out.println("✅ Estadísticas agrupadas funcionan correctamente");
    }

    @Test
    void loanBusinessMethods_ShouldWorkCorrectly() {
        System.out.println("🧪 Repository Test: Métodos de negocio en entidad Loan");

        // Test isOverdue()
        assertFalse(activeLoan.isOverdue(), "Préstamo activo no debe estar vencido");
        assertTrue(overdueLoan.isOverdue(), "Préstamo con fecha pasada debe estar vencido");
        assertFalse(returnedLoan.isOverdue(), "Préstamo devuelto no cuenta como vencido");

        // Test canBeReturned()
        assertTrue(activeLoan.canBeReturned(), "Préstamo activo puede devolverse");
        assertTrue(overdueLoan.canBeReturned(), "Préstamo vencido puede devolverse");
        assertFalse(returnedLoan.canBeReturned(), "Préstamo devuelto no puede devolverse otra vez");

        // Test getDaysOverdue()
        assertEquals(0, activeLoan.getDaysOverdue(), "Préstamo no vencido = 0 días");
        assertTrue(overdueLoan.getDaysOverdue() > 0, "Préstamo vencido debe tener días > 0");
        assertEquals(0, returnedLoan.getDaysOverdue(), "Préstamo devuelto = 0 días vencidos");

        // Test getLoanDurationDays()
        assertTrue(activeLoan.getLoanDurationDays() > 0, "Duración debe ser positiva");

        System.out.println("✅ Métodos de negocio funcionan correctamente");
    }

    @Test
    void crudOperations_ShouldPersistChanges() {
        System.out.println("🧪 Repository Test: Operaciones CRUD completas");

        // UPDATE - Modificar notas del préstamo activo
        String originalNotes = activeLoan.getNotes();
        activeLoan.setNotes("Notas actualizadas en test de integración");
        Loan updatedLoan = loanRepository.save(activeLoan);

        assertEquals("Notas actualizadas en test de integración", updatedLoan.getNotes());
        assertNotEquals(originalNotes, updatedLoan.getNotes());

        // Verificar que persiste al recargar
        Loan reloadedLoan = loanRepository.findById(activeLoan.getId()).orElseThrow();
        assertEquals("Notas actualizadas en test de integración", reloadedLoan.getNotes());

        // DELETE - Eliminar préstamo devuelto
        Long returnedLoanId = returnedLoan.getId();
        assertTrue(loanRepository.existsById(returnedLoanId));

        loanRepository.deleteById(returnedLoanId);
        assertFalse(loanRepository.existsById(returnedLoanId));

        // Verificar que otros préstamos siguen existiendo
        assertEquals(2, loanRepository.count());

        System.out.println("✅ Operaciones CRUD funcionan correctamente");
    }

    @Test
    void findByUserIdAndStatus_ShouldCombineFilters() {
        System.out.println("🧪 Repository Test: Combinar filtros usuario + estado");

        // Usuario 1 con préstamos activos
        List<Loan> user1Active = loanRepository.findByUserIdAndStatus(1L, LoanStatus.ACTIVE);
        assertEquals(2, user1Active.size()); // activeLoan y overdueLoan

        // Usuario 2 con préstamos devueltos
        List<Loan> user2Returned = loanRepository.findByUserIdAndStatus(2L, LoanStatus.RETURNED);
        assertEquals(1, user2Returned.size());

        // Usuario 1 con préstamos devueltos (no tiene)
        List<Loan> user1Returned = loanRepository.findByUserIdAndStatus(1L, LoanStatus.RETURNED);
        assertTrue(user1Returned.isEmpty());

        System.out.println("✅ Filtros combinados funcionan correctamente");
    }

    @Test
    void findByUserIdAndStatusIn_ShouldHandleMultipleStatuses() {
        System.out.println("🧪 Repository Test: Filtrar por múltiples estados");

        // Crear préstamo OVERDUE adicional para el usuario 1
        Loan additionalOverdue = new Loan();
        additionalOverdue.setUserId(1L);
        additionalOverdue.setBookId(201L);
        additionalOverdue.setLoanDate(LocalDate.now().minusDays(30));
        additionalOverdue.setDueDate(LocalDate.now().minusDays(10));
        additionalOverdue.setStatus(LoanStatus.OVERDUE);
        additionalOverdue.setNotes("Préstamo vencido adicional");
        loanRepository.save(additionalOverdue);

        // Buscar préstamos ACTIVE u OVERDUE para usuario 1
        List<LoanStatus> activeStatuses = List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
        List<Loan> activeOrOverdue = loanRepository.findByUserIdAndStatusIn(1L, activeStatuses);

        assertEquals(3, activeOrOverdue.size()); // activeLoan + overdueLoan + additionalOverdue
        assertTrue(activeOrOverdue.stream().allMatch(loan -> loan.getUserId().equals(1L)));
        assertTrue(activeOrOverdue.stream().allMatch(loan ->
                loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE));

        System.out.println("✅ Filtrado por múltiples estados funciona correctamente");
    }

    @Test
    void findByLoanDateBetween_ShouldFilterByDateRange() {
        System.out.println("🧪 Repository Test: Filtrar por rango de fechas");

        // Definir rango de fechas
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().minusDays(1);

        // Buscar préstamos en el rango
        List<Loan> loansInRange = loanRepository.findByLoanDateBetween(startDate, endDate);

        // Verificar que los resultados están en el rango
        assertTrue(loansInRange.stream().allMatch(loan ->
                !loan.getLoanDate().isBefore(startDate) && !loan.getLoanDate().isAfter(endDate)));

        // Debería incluir activeLoan (creado hace 5 días)
        assertTrue(loansInRange.stream().anyMatch(loan -> loan.getId().equals(activeLoan.getId())));

        System.out.println("✅ Filtrado por rango de fechas funciona correctamente");
    }
}