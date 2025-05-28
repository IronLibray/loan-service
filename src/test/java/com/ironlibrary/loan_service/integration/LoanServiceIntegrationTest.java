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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests de integración SIMPLIFICADOS para LoanService
 * Sin dependencias complejas de Spring Cloud
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false"
})
class LoanServiceIntegrationTest {

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
        System.out.println("🔧 Configurando test de integración");

        // Reset mocks before each test
        reset(userServiceClient, bookServiceClient);

        // Clear database
        loanRepository.deleteAll();

        // Setup test user
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@email.com");
        testUser.setMembershipType("PREMIUM");
        testUser.setIsActive(true);

        // Setup test book
        testBook = new BookDto();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setAvailableCopies(5);

        System.out.println("✅ Setup completado");
    }

    @Test
    void createLoan_ShouldIntegrateWithServices() {
        System.out.println("🧪 Probando creación de préstamo con integración de servicios");

        // Given - Mock responses from other services
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        // When - Create loan
        Loan createdLoan = loanService.createLoan(1L, 1L, "Integration test loan");

        // Then - Verify loan was created
        assertNotNull(createdLoan);
        assertEquals(1L, createdLoan.getUserId());
        assertEquals(1L, createdLoan.getBookId());
        assertEquals(LoanStatus.ACTIVE, createdLoan.getStatus());
        assertEquals(LocalDate.now(), createdLoan.getLoanDate());
        assertEquals(LocalDate.now().plusDays(30), createdLoan.getDueDate()); // Premium = 30 days
        assertEquals("Integration test loan", createdLoan.getNotes());

        // Verify interactions with external services
        verify(userServiceClient).getUserById(1L);
        verify(userServiceClient).validateUser(1L);
        verify(bookServiceClient).getBookById(1L);
        verify(bookServiceClient).isBookAvailable(1L);
        verify(bookServiceClient).updateAvailability(1L, -1);

        // Verify loan was persisted
        assertTrue(loanRepository.findById(createdLoan.getId()).isPresent());

        System.out.println("✅ Préstamo creado e integrado correctamente");
    }

    @Test
    void returnBook_ShouldIntegrateWithBookService() {
        System.out.println("🧪 Probando devolución con integración al Book Service");

        // Given - Create a loan first
        Loan loan = new Loan();
        loan.setUserId(1L);
        loan.setBookId(1L);
        loan.setLoanDate(LocalDate.now().minusDays(5));
        loan.setDueDate(LocalDate.now().plusDays(10));
        loan.setStatus(LoanStatus.ACTIVE);
        loan = loanRepository.save(loan);

        // When - Return the book
        Loan returnedLoan = loanService.returnBook(loan.getId());

        // Then - Verify return and book service interaction
        assertNotNull(returnedLoan);
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
        assertNotNull(returnedLoan.getReturnDate());
        assertEquals(LocalDate.now(), returnedLoan.getReturnDate());

        // Verify book availability was updated
        verify(bookServiceClient).updateAvailability(1L, 1);

        System.out.println("✅ Devolución integrada correctamente");
    }

    @Test
    void createLoan_ShouldRespectUserLimits() {
        System.out.println("🧪 Probando límites de usuario");

        // Given - User at limit
        testUser.setMembershipType("BASIC"); // Basic = 3 books max
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        // Create 3 existing loans to reach limit
        for (int i = 0; i < 3; i++) {
            Loan existingLoan = new Loan();
            existingLoan.setUserId(1L);
            existingLoan.setBookId((long) (i + 10)); // Different book IDs
            existingLoan.setLoanDate(LocalDate.now().minusDays(1));
            existingLoan.setDueDate(LocalDate.now().plusDays(13));
            existingLoan.setStatus(LoanStatus.ACTIVE);
            loanRepository.save(existingLoan);
        }

        // When & Then - Should reject loan
        assertThrows(Exception.class, () -> loanService.createLoan(1L, 1L));

        // Verify book service was not called to update availability
        verify(bookServiceClient, never()).updateAvailability(anyLong(), anyInt());

        System.out.println("✅ Límites de usuario respetados");
    }

    @Test
    void findAllLoans_ShouldReturnAllLoans() {
        System.out.println("🧪 Probando búsqueda de todos los préstamos");

        // Given - Create some loans
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(2L, 2L, LoanStatus.RETURNED);

        // When
        var loans = loanService.findAllLoans();

        // Then
        assertNotNull(loans);
        assertEquals(2, loans.size());

        System.out.println("✅ Todos los préstamos encontrados: " + loans.size());
    }

    @Test
    void getLoanStatistics_ShouldReturnCorrectStats() {
        System.out.println("🧪 Probando estadísticas de préstamos");

        // Given - Create loans with different statuses
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(2L, 2L, LoanStatus.RETURNED);
        createLoanInDatabase(3L, 3L, LoanStatus.OVERDUE);

        // When
        var stats = loanService.getLoanStatistics();

        // Then
        assertEquals(3L, stats.totalLoans);
        assertEquals(1L, stats.activeLoans);
        assertEquals(1L, stats.returnedLoans);
        assertEquals(1L, stats.overdueLoans);

        System.out.println("✅ Estadísticas correctas - Total: " + stats.totalLoans +
                ", Activos: " + stats.activeLoans +
                ", Devueltos: " + stats.returnedLoans +
                ", Vencidos: " + stats.overdueLoans);
    }

    @Test
    void createLoan_ShouldHandleUserServiceFailure() {
        System.out.println("🧪 Probando manejo de fallos del User Service");

        // Given - User service returns error
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("User service unavailable"));

        // When & Then - Should handle gracefully
        assertThrows(Exception.class, () -> loanService.createLoan(1L, 1L));

        // Verify no loan was created
        assertEquals(0, loanRepository.count());

        // Verify book service was not called
        verify(bookServiceClient, never()).updateAvailability(anyLong(), anyInt());

        System.out.println("✅ Fallo del User Service manejado correctamente");
    }

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