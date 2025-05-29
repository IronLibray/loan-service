package com.ironlibrary.loan_service.service;

import com.ironlibrary.loan_service.exception.LoanNotFoundException;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.openfeign.client.config.default.connect-timeout=1000",
        "spring.cloud.openfeign.client.config.default.read-timeout=1000",
        "eureka.client.enabled=false"
})
@Transactional // Rollback automático después de cada test
class LoanServiceIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;


    @BeforeEach
    void setUp() {
        System.out.println("🧪 === Service Integration Test SIMPLE - Setup ===");

        // Limpiar base de datos
        loanRepository.deleteAll();

        System.out.println("✅ Configuración completada - BD limpia");
    }

    @Test
    void loanRepository_ShouldPersistAndRetrieveLoans() {
        System.out.println("🧪 Service Integration: Persistencia y recuperación básica");

        // Given - Crear préstamo directamente
        Loan loan = createTestLoan(1L, 101L, "Préstamo de integración", LoanStatus.ACTIVE);

        // When - Guardar en BD
        Loan savedLoan = loanRepository.save(loan);

        // Then - Verificar que se guardó correctamente
        assertNotNull(savedLoan.getId());
        assertEquals(1L, savedLoan.getUserId());
        assertEquals(101L, savedLoan.getBookId());
        assertEquals(LoanStatus.ACTIVE, savedLoan.getStatus());

        // Verificar que se puede recuperar
        Loan retrievedLoan = loanRepository.findById(savedLoan.getId()).orElseThrow();
        assertEquals(savedLoan.getUserId(), retrievedLoan.getUserId());
        assertEquals(savedLoan.getBookId(), retrievedLoan.getBookId());

        System.out.println("✅ Persistencia y recuperación funciona correctamente");
    }

    @Test
    void findLoansByUser_ShouldReturnUserLoans() {
        System.out.println("🧪 Service Integration: Buscar préstamos por usuario");

        // Given - Crear préstamos para diferentes usuarios
        Loan loan1 = createTestLoan(1L, 101L, "Usuario 1 - Libro 1", LoanStatus.ACTIVE);
        Loan loan2 = createTestLoan(1L, 102L, "Usuario 1 - Libro 2", LoanStatus.ACTIVE);
        Loan loan3 = createTestLoan(2L, 103L, "Usuario 2 - Libro 3", LoanStatus.ACTIVE);

        loanRepository.save(loan1);
        loanRepository.save(loan2);
        loanRepository.save(loan3);

        // When - Buscar préstamos del usuario 1
        List<Loan> user1Loans = loanRepository.findByUserId(1L);

        // Then - Verificar resultados
        assertEquals(2, user1Loans.size());
        assertTrue(user1Loans.stream().allMatch(loan -> loan.getUserId().equals(1L)));

        // Verificar que contiene los libros correctos
        List<Long> bookIds = user1Loans.stream().map(Loan::getBookId).toList();
        assertTrue(bookIds.contains(101L));
        assertTrue(bookIds.contains(102L));

        // When - Buscar préstamos del usuario 2
        List<Loan> user2Loans = loanRepository.findByUserId(2L);

        // Then - Verificar aislamiento de datos
        assertEquals(1, user2Loans.size());
        assertEquals(2L, user2Loans.get(0).getUserId());
        assertEquals(103L, user2Loans.get(0).getBookId());

        System.out.println("✅ Búsqueda por usuario funciona correctamente");
    }

    @Test
    void findActiveLoansForUser_ShouldFilterByStatus() {
        System.out.println("🧪 Service Integration: Filtrar préstamos activos");

        // Given - Crear préstamos con diferentes estados
        Loan activeLoan = createTestLoan(1L, 101L, "Activo", LoanStatus.ACTIVE);
        Loan returnedLoan = createTestLoan(1L, 102L, "Devuelto", LoanStatus.RETURNED);
        returnedLoan.setReturnDate(LocalDate.now().minusDays(1));
        Loan overdueLoan = createTestLoan(1L, 103L, "Vencido", LoanStatus.OVERDUE);

        loanRepository.save(activeLoan);
        loanRepository.save(returnedLoan);
        loanRepository.save(overdueLoan);

        // When - Buscar préstamos activos del usuario 1
        List<Loan> activeLoans = loanRepository.findByUserIdAndStatus(1L, LoanStatus.ACTIVE);

        // Then - Solo debe retornar el préstamo activo
        assertEquals(1, activeLoans.size());
        assertEquals(activeLoan.getId(), activeLoans.get(0).getId());
        assertEquals(LoanStatus.ACTIVE, activeLoans.get(0).getStatus());

        System.out.println("✅ Filtrado por estado funciona correctamente");
    }

    @Test
    void updateLoanStatus_ShouldPersistChanges() {
        System.out.println("🧪 Service Integration: Actualizar estado de préstamo");

        // Given - Crear préstamo activo
        Loan loan = createTestLoan(1L, 101L, "Para devolver", LoanStatus.ACTIVE);
        Loan savedLoan = loanRepository.save(loan);

        // When - Simular devolución del libro
        savedLoan.setStatus(LoanStatus.RETURNED);
        savedLoan.setReturnDate(LocalDate.now());
        Loan updatedLoan = loanRepository.save(savedLoan);

        // Then - Verificar cambios en memoria
        assertEquals(LoanStatus.RETURNED, updatedLoan.getStatus());
        assertNotNull(updatedLoan.getReturnDate());

        // Verificar que se persistió en BD
        Loan dbLoan = loanRepository.findById(savedLoan.getId()).orElseThrow();
        assertEquals(LoanStatus.RETURNED, dbLoan.getStatus());
        assertNotNull(dbLoan.getReturnDate());

        System.out.println("✅ Actualización de estado funciona correctamente");
    }

    @Test
    void extendLoanDueDate_ShouldUpdateDatabase() {
        System.out.println("🧪 Service Integration: Extender fecha de vencimiento");

        // Given - Crear préstamo
        Loan loan = createTestLoan(1L, 101L, "Para extender", LoanStatus.ACTIVE);
        Loan savedLoan = loanRepository.save(loan);
        LocalDate originalDueDate = savedLoan.getDueDate();

        // When - Extender fecha de vencimiento
        savedLoan.setDueDate(originalDueDate.plusDays(14));
        Loan extendedLoan = loanRepository.save(savedLoan);

        // Then - Verificar nueva fecha
        assertEquals(originalDueDate.plusDays(14), extendedLoan.getDueDate());

        // Verificar persistencia en BD
        Loan dbLoan = loanRepository.findById(savedLoan.getId()).orElseThrow();
        assertEquals(originalDueDate.plusDays(14), dbLoan.getDueDate());

        System.out.println("✅ Extensión de fecha funciona correctamente");
    }

    @Test
    void findOverdueLoans_ShouldIdentifyVencidos() {
        System.out.println("🧪 Service Integration: Identificar préstamos vencidos");

        // Given - Crear préstamos diversos
        Loan activeLoan = createTestLoan(1L, 101L, "Activo normal", LoanStatus.ACTIVE);
        activeLoan.setDueDate(LocalDate.now().plusDays(5)); // No vencido

        Loan overdueLoan = createTestLoan(2L, 102L, "Vencido", LoanStatus.ACTIVE);
        overdueLoan.setDueDate(LocalDate.now().minusDays(7)); // Vencido hace 7 días

        Loan returnedLoan = createTestLoan(3L, 103L, "Devuelto", LoanStatus.RETURNED);
        returnedLoan.setDueDate(LocalDate.now().minusDays(2)); // Era vencido pero ya devuelto

        loanRepository.save(activeLoan);
        loanRepository.save(overdueLoan);
        loanRepository.save(returnedLoan);

        // When - Buscar préstamos vencidos usando query personalizada
        List<Loan> overdueLoans = loanRepository.findOverdueLoans();

        // Then - Solo debe encontrar el préstamo vencido activo
        assertEquals(1, overdueLoans.size());
        assertEquals(overdueLoan.getId(), overdueLoans.get(0).getId());
        assertTrue(overdueLoans.get(0).getDueDate().isBefore(LocalDate.now()));
        assertEquals(LoanStatus.ACTIVE, overdueLoans.get(0).getStatus());

        System.out.println("✅ Identificación de préstamos vencidos funciona correctamente");
    }

    @Test
    void loanBusinessLogic_ShouldWorkCorrectly() {
        System.out.println("🧪 Service Integration: Lógica de negocio de préstamos");

        // Given - Crear préstamos con diferentes estados
        Loan activeLoan = createTestLoan(1L, 101L, "Activo", LoanStatus.ACTIVE);
        activeLoan.setDueDate(LocalDate.now().plusDays(5));

        Loan overdueLoan = createTestLoan(2L, 102L, "Vencido", LoanStatus.ACTIVE);
        overdueLoan.setDueDate(LocalDate.now().minusDays(3));

        Loan returnedLoan = createTestLoan(3L, 103L, "Devuelto", LoanStatus.RETURNED);
        returnedLoan.setReturnDate(LocalDate.now().minusDays(1));

        // When & Then - Test métodos de negocio
        // isOverdue()
        assertFalse(activeLoan.isOverdue(), "Préstamo activo no vencido");
        assertTrue(overdueLoan.isOverdue(), "Préstamo activo vencido");
        assertFalse(returnedLoan.isOverdue(), "Préstamo devuelto no cuenta como vencido");

        // canBeReturned()
        assertTrue(activeLoan.canBeReturned(), "Préstamo activo puede devolverse");
        assertTrue(overdueLoan.canBeReturned(), "Préstamo vencido puede devolverse");
        assertFalse(returnedLoan.canBeReturned(), "Préstamo devuelto no puede devolverse otra vez");

        // getDaysOverdue()
        assertEquals(0, activeLoan.getDaysOverdue(), "Préstamo no vencido = 0 días");
        assertTrue(overdueLoan.getDaysOverdue() > 0, "Préstamo vencido debe tener días > 0");
        assertEquals(0, returnedLoan.getDaysOverdue(), "Préstamo devuelto = 0 días");

        System.out.println("✅ Lógica de negocio funciona correctamente");
    }

    @Test
    void getLoanStatistics_ShouldCalculateCorrectly() {
        System.out.println("🧪 Service Integration: Estadísticas de préstamos");

        // Given - Crear préstamos con diferentes estados
        loanRepository.save(createTestLoan(1L, 101L, "Activo 1", LoanStatus.ACTIVE));
        loanRepository.save(createTestLoan(2L, 102L, "Activo 2", LoanStatus.ACTIVE));

        Loan returnedLoan = createTestLoan(3L, 103L, "Devuelto", LoanStatus.RETURNED);
        returnedLoan.setReturnDate(LocalDate.now().minusDays(1));
        loanRepository.save(returnedLoan);

        loanRepository.save(createTestLoan(4L, 104L, "Vencido", LoanStatus.OVERDUE));

        // When - Obtener estadísticas usando query personalizada
        List<Object[]> stats = loanRepository.getLoanStatistics();

        // Then - Verificar estadísticas
        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // Convertir a conteos por estado
        long activeCount = 0;
        long returnedCount = 0;
        long overdueCount = 0;

        for (Object[] stat : stats) {
            LoanStatus status = (LoanStatus) stat[0];
            Long count = (Long) stat[1];

            switch (status) {
                case ACTIVE -> activeCount = count;
                case RETURNED -> returnedCount = count;
                case OVERDUE -> overdueCount = count;
            }
        }

        assertEquals(2L, activeCount, "Debería haber 2 préstamos activos");
        assertEquals(1L, returnedCount, "Debería haber 1 préstamo devuelto");
        assertEquals(1L, overdueCount, "Debería haber 1 préstamo vencido");

        // Verificar total
        long totalLoans = loanRepository.count();
        assertEquals(4L, totalLoans, "Total debería ser 4 préstamos");

        System.out.println("✅ Estadísticas funcionan correctamente");
    }

    @Test
    void complexQueries_ShouldWorkWithRealDatabase() {
        System.out.println("🧪 Service Integration: Queries complejas con BD real");

        // Given - Crear préstamos diversos para usuario 1
        Loan loan1 = createTestLoan(1L, 101L, "Libro 1", LoanStatus.ACTIVE);
        Loan loan2 = createTestLoan(1L, 102L, "Libro 2", LoanStatus.ACTIVE);
        Loan loan3 = createTestLoan(1L, 103L, "Libro 3", LoanStatus.RETURNED);
        loan3.setReturnDate(LocalDate.now().minusDays(5));

        loanRepository.save(loan1);
        loanRepository.save(loan2);
        loanRepository.save(loan3);

        // Test 1: Contar préstamos activos por usuario
        Long activeCount = loanRepository.countActiveLoansForUser(1L);
        assertEquals(2L, activeCount);

        // Test 2: Verificar si usuario tiene préstamo activo de un libro específico
        assertTrue(loanRepository.hasActiveLoanForBook(1L, 101L));
        assertTrue(loanRepository.hasActiveLoanForBook(1L, 102L));
        assertFalse(loanRepository.hasActiveLoanForBook(1L, 103L)); // Está devuelto

        // Test 3: Buscar préstamos por múltiples estados
        List<LoanStatus> activeStatuses = List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
        List<Loan> activeOrOverdue = loanRepository.findByUserIdAndStatusIn(1L, activeStatuses);
        assertEquals(2, activeOrOverdue.size());

        System.out.println("✅ Queries complejas funcionan correctamente");
    }

    @Test
    void transactionBehavior_ShouldWorkCorrectly() {
        System.out.println("🧪 Service Integration: Comportamiento transaccional correcto");

        // Given - Verificar estado inicial
        long countBefore = loanRepository.count();
        System.out.println("📊 Préstamos al inicio: " + countBefore);

        // When - Realizar operaciones dentro de la transacción del test
        Loan loan = createTestLoan(1L, 101L, "Test transacción", LoanStatus.ACTIVE);
        loanRepository.save(loan);

        // Then - Verificar que se guardó DURANTE el test
        long countDuring = loanRepository.count();
        System.out.println("📊 Préstamos durante test: " + countDuring);
        assertEquals(countBefore + 1, countDuring, "El préstamo debe guardarse durante el test");


        System.out.println("✅ Comportamiento transaccional verificado");
        System.out.println("🔄 Rollback automático ocurrirá al finalizar el test");
    }

    @Test
    void testIsolation_ShouldStartWithCleanDatabase() {
        System.out.println("🧪 Service Integration: Verificar aislamiento entre tests");


        long count = loanRepository.count();
        assertEquals(0, count, "Cada test debe comenzar con BD limpia (rollback automático funcionando)");

        // Crear préstamos en este test para verificar que funciona
        Loan loan1 = createTestLoan(1L, 101L, "Aislamiento 1", LoanStatus.ACTIVE);
        Loan loan2 = createTestLoan(2L, 102L, "Aislamiento 2", LoanStatus.RETURNED);
        loan2.setReturnDate(LocalDate.now().minusDays(1));

        loanRepository.save(loan1);
        loanRepository.save(loan2);

        // Verificar que se guardaron
        assertEquals(2, loanRepository.count(), "Los préstamos deben guardarse durante este test");

        System.out.println("✅ Aislamiento entre tests funciona correctamente");
        System.out.println("🔄 Estos préstamos se eliminarán automáticamente al finalizar");
    }

    @Test
    void transactionScope_ShouldDemonstrateCorrectBehavior() {
        System.out.println("🧪 Service Integration: Demostrar alcance de transacciones");

        // Verificar inicio limpio
        assertEquals(0, loanRepository.count(), "Inicio: BD debe estar limpia");

        // Realizar múltiples operaciones en la misma transacción
        System.out.println("📝 Realizando operaciones múltiples...");

        // Operación 1: Crear
        Loan loan1 = createTestLoan(1L, 101L, "Demo transacción 1", LoanStatus.ACTIVE);
        loanRepository.save(loan1);
        assertEquals(1, loanRepository.count(), "Después de crear préstamo 1");

        // Operación 2: Crear otro
        Loan loan2 = createTestLoan(2L, 102L, "Demo transacción 2", LoanStatus.ACTIVE);
        loanRepository.save(loan2);
        assertEquals(2, loanRepository.count(), "Después de crear préstamo 2");

        // Operación 3: Actualizar
        loan1.setNotes("Actualizado en transacción");
        loan1.setStatus(LoanStatus.RETURNED);
        loan1.setReturnDate(LocalDate.now());
        loanRepository.save(loan1);
        assertEquals(2, loanRepository.count(), "Después de actualizar préstamo 1");

        // Operación 4: Eliminar
        loanRepository.delete(loan2);
        assertEquals(1, loanRepository.count(), "Después de eliminar préstamo 2");

        // Verificar estado final dentro del test
        List<Loan> remainingLoans = loanRepository.findAll();
        assertEquals(1, remainingLoans.size());
        assertEquals("Actualizado en transacción", remainingLoans.get(0).getNotes());
        assertEquals(LoanStatus.RETURNED, remainingLoans.get(0).getStatus());

        System.out.println("✅ Todas las operaciones ejecutadas en la misma transacción");
        System.out.println("🔄 Rollback automático al finalizar eliminará todos los cambios");
    }

    // ✅ HELPER METHODS

    private Loan createTestLoan(Long userId, Long bookId, String notes, LoanStatus status) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setLoanDate(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus(status);
        loan.setNotes(notes);
        return loan;
    }
}
