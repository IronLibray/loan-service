package com.ironlibrary.loan_service.integration;

import com.ironlibrary.loan_service.client.BookServiceClient;
import com.ironlibrary.loan_service.client.UserServiceClient;
import com.ironlibrary.loan_service.client.dto.BookDto;
import com.ironlibrary.loan_service.client.dto.UserDto;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.repository.LoanRepository;
import com.ironlibrary.loan_service.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FixedIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanRepository loanRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private BookServiceClient bookServiceClient;

    private UserDto testUser;
    private BookDto testBook;

    @BeforeEach
    void setUp() {
        System.out.println("🔧 Configurando test de integración CORREGIDO");

        // Limpiar base de datos
        loanRepository.deleteAll();

        // Setup user mock
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setName("Usuario Test");
        testUser.setEmail("test@example.com");
        testUser.setMembershipType("PREMIUM");
        testUser.setIsActive(true);

        // Setup book mock
        testBook = new BookDto();
        testBook.setId(1L);
        testBook.setTitle("Libro Test");
        testBook.setAuthor("Autor Test");
        testBook.setAvailableCopies(5);

        System.out.println("✅ Setup completado - BD limpia: " + loanRepository.count() + " préstamos");
    }

    @Test
    void contextLoads() {
        System.out.println("🧪 Test: Context Loads");

        assertNotNull(loanService);
        assertNotNull(loanRepository);
        assertNotNull(userServiceClient);
        assertNotNull(bookServiceClient);

        System.out.println("✅ Contexto de Spring cargado correctamente");
    }

    @Test
    void createLoan_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Create Loan with Database Integration");

        // Given - Setup mocks
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        doNothing().when(bookServiceClient).updateAvailability(anyLong(), any(Integer.class));

        // When - Create loan
        Loan createdLoan = loanService.createLoan(1L, 1L, "Test integration loan");

        // Then - Verify loan was created and persisted
        assertNotNull(createdLoan);
        assertNotNull(createdLoan.getId()); // ID asignado por base de datos
        assertEquals(1L, createdLoan.getUserId());
        assertEquals(1L, createdLoan.getBookId());
        assertEquals(LoanStatus.ACTIVE, createdLoan.getStatus());
        assertEquals("Test integration loan", createdLoan.getNotes());
        assertNotNull(createdLoan.getLoanDate());
        assertNotNull(createdLoan.getDueDate());

        // Verify in database
        assertEquals(1L, loanRepository.count());
        assertTrue(loanRepository.findById(createdLoan.getId()).isPresent());

        // Verify external service calls
        verify(userServiceClient).getUserById(1L);
        verify(userServiceClient).validateUser(1L);
        verify(bookServiceClient).getBookById(1L);
        verify(bookServiceClient).isBookAvailable(1L);
        verify(bookServiceClient).updateAvailability(1L, -1);

        System.out.println("✅ Préstamo creado e integrado con base de datos");
    }

    @Test
    void returnBook_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Return Book with Database Integration");

        // Given - Create loan directly in database
        Loan loan = new Loan();
        loan.setUserId(1L);
        loan.setBookId(1L);
        loan.setLoanDate(LocalDate.now().minusDays(5));
        loan.setDueDate(LocalDate.now().plusDays(10));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setNotes("Loan to be returned");
        loan = loanRepository.save(loan);
        Long loanId = loan.getId();

        // When - Return the book
        Loan returnedLoan = loanService.returnBook(loanId);

        // Then - Verify return
        assertNotNull(returnedLoan);
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
        assertNotNull(returnedLoan.getReturnDate());
        assertEquals(LocalDate.now(), returnedLoan.getReturnDate());

        // Verify in database
        Loan dbLoan = loanRepository.findById(loanId).orElse(null);
        assertNotNull(dbLoan);
        assertEquals(LoanStatus.RETURNED, dbLoan.getStatus());

        // Verify book service was called to restore availability
        verify(bookServiceClient).updateAvailability(1L, 1);

        System.out.println("✅ Libro devuelto e integrado con base de datos");
    }

    @Test
    void findAllLoans_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Find All Loans with Database");

        // Given - Create multiple loans in database
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(2L, 2L, LoanStatus.RETURNED);
        createLoanInDatabase(3L, 3L, LoanStatus.OVERDUE);

        // When
        var allLoans = loanService.findAllLoans();

        // Then
        assertNotNull(allLoans);
        assertEquals(3, allLoans.size());
        assertEquals(3L, loanRepository.count());

        System.out.println("✅ Búsqueda de todos los préstamos funcionando: " + allLoans.size() + " encontrados");
    }

    @Test
    void getLoanStatistics_ShouldReflectDatabaseData() {
        System.out.println("🧪 Test: Loan Statistics with Real Database Data");

        // Given - Create loans with different statuses
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(2L, 2L, LoanStatus.ACTIVE);
        createLoanInDatabase(3L, 3L, LoanStatus.RETURNED);
        createLoanInDatabase(4L, 4L, LoanStatus.OVERDUE);

        // When
        var stats = loanService.getLoanStatistics();

        // Then
        assertEquals(4L, stats.totalLoans);
        assertEquals(2L, stats.activeLoans);
        assertEquals(1L, stats.returnedLoans);
        assertEquals(1L, stats.overdueLoans);

        System.out.println("✅ Estadísticas correctas - Total: " + stats.totalLoans +
                ", Activos: " + stats.activeLoans +
                ", Devueltos: " + stats.returnedLoans +
                ", Vencidos: " + stats.overdueLoans);
    }

    @Test
    void findLoansByUser_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Find Loans By User with Database");

        // Given - Mock user service
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // Create loans for different users
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(1L, 2L, LoanStatus.RETURNED);
        createLoanInDatabase(2L, 3L, LoanStatus.ACTIVE); // Different user

        // When
        var userLoans = loanService.findLoansByUser(1L);

        // Then
        assertNotNull(userLoans);
        assertEquals(2, userLoans.size()); // Only loans for user 1
        assertTrue(userLoans.stream().allMatch(loan -> loan.getUserId().equals(1L)));

        verify(userServiceClient).getUserById(1L);
        System.out.println("✅ Búsqueda de préstamos por usuario funcionando: " + userLoans.size() + " encontrados");
    }

    @Test
    void findActiveLoansForUser_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Find Active Loans For User with Database");

        // Given - Create active and returned loans for user
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(1L, 2L, LoanStatus.RETURNED);
        createLoanInDatabase(1L, 3L, LoanStatus.ACTIVE);

        // When
        var activeLoans = loanService.findActiveLoansForUser(1L);

        // Then
        assertNotNull(activeLoans);
        assertEquals(2, activeLoans.size());
        assertTrue(activeLoans.stream().allMatch(loan ->
                loan.getUserId().equals(1L) && loan.getStatus() == LoanStatus.ACTIVE));

        System.out.println("✅ Búsqueda de préstamos activos funcionando: " + activeLoans.size() + " encontrados");
    }

    @Test
    void extendLoan_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Extend Loan with Database");

        // Given - Create active loan
        Loan loan = createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        LocalDate originalDueDate = loan.getDueDate();

        // When
        Loan extendedLoan = loanService.extendLoan(loan.getId(), 7);

        // Then
        assertNotNull(extendedLoan);
        assertEquals(originalDueDate.plusDays(7), extendedLoan.getDueDate());
        assertEquals(LoanStatus.ACTIVE, extendedLoan.getStatus());

        // Verify in database
        Loan dbLoan = loanRepository.findById(loan.getId()).orElse(null);
        assertNotNull(dbLoan);
        assertEquals(originalDueDate.plusDays(7), dbLoan.getDueDate());

        System.out.println("✅ Extensión de préstamo funcionando");
    }

    @Test
    void deleteLoan_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Delete Loan with Database");

        // Given - Create returned loan (can be deleted)
        Loan loan = createLoanInDatabase(1L, 1L, LoanStatus.RETURNED);
        Long loanId = loan.getId();

        // Verify loan exists
        assertTrue(loanRepository.existsById(loanId));

        // When
        loanService.deleteLoan(loanId);

        // Then - Verify loan was deleted
        assertFalse(loanRepository.existsById(loanId));
        assertEquals(0L, loanRepository.count());

        System.out.println("✅ Eliminación de préstamo funcionando");
    }

    @Test
    void createLoan_ShouldRespectUserLimits() {
        System.out.println("🧪 Test: User Limits with Database");

        // Given - Basic user (limit: 3)
        testUser.setMembershipType("BASIC");
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(anyLong())).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(anyLong())).thenReturn(true);

        // Create 3 loans (at limit)
        for (int i = 1; i <= 3; i++) {
            createLoanInDatabase(1L, (long) i, LoanStatus.ACTIVE);
        }

        // When & Then - 4th loan should fail
        Exception exception = assertThrows(Exception.class, () ->
                loanService.createLoan(1L, 4L));

        assertTrue(exception.getMessage().contains("límite"));
        assertEquals(3L, loanRepository.count()); // Still only 3 loans

        System.out.println("✅ Límites de usuario respetados");
    }

    @Test
    void createLoan_ShouldPreventDuplicateBooks() {
        System.out.println("🧪 Test: Prevent Duplicate Books with Database");

        // Given - Setup mocks
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        // Create existing loan for same book
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);

        // When & Then - Second loan for same book should fail
        Exception exception = assertThrows(Exception.class, () ->
                loanService.createLoan(1L, 1L));

        assertTrue(exception.getMessage().contains("ya tiene este libro prestado"));
        assertEquals(1L, loanRepository.count()); // Still only 1 loan

        System.out.println("✅ Prevención de libros duplicados funcionando");
    }

    @Test
    void createLoan_ShouldHandleUserServiceFailure() {
        System.out.println("🧪 Test: Handle User Service Failure");

        // Given - User service returns error
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("User service unavailable"));

        // When & Then - Should handle gracefully
        Exception exception = assertThrows(Exception.class, () ->
                loanService.createLoan(1L, 1L));

        assertTrue(exception.getMessage().contains("Usuario no válido") ||
                exception.getMessage().contains("User service unavailable"));
        assertEquals(0L, loanRepository.count()); // No loan created

        // Verify book service was not called
        verify(bookServiceClient, never()).updateAvailability(anyLong(), any(Integer.class));

        System.out.println("✅ Fallo del User Service manejado correctamente");
    }

    @Test
    void findLoanById_ShouldWorkWithDatabase() {
        System.out.println("🧪 Test: Find Loan By ID with Database");

        // Given - Create loan in database
        Loan loan = createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        Long loanId = loan.getId();

        // When
        Loan foundLoan = loanService.findLoanById(loanId);

        // Then
        assertNotNull(foundLoan);
        assertEquals(loanId, foundLoan.getId());
        assertEquals(1L, foundLoan.getUserId());
        assertEquals(1L, foundLoan.getBookId());
        assertEquals(LoanStatus.ACTIVE, foundLoan.getStatus());

        System.out.println("✅ Búsqueda por ID funcionando");
    }

    /**
     * Helper method to create loans directly in database
     */
    private Loan createLoanInDatabase(Long userId, Long bookId, LoanStatus status) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setLoanDate(LocalDate.now().minusDays(5));
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus(status);
        loan.setNotes("Test loan");

        if (status == LoanStatus.RETURNED) {
            loan.setReturnDate(LocalDate.now().minusDays(1));
        }

        return loanRepository.save(loan);
    }
}
