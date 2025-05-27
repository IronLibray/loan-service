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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests de integración para LoanService que prueban la comunicación
 * con otros microservicios usando Feign Clients
 * Compatible con Spring Boot 3.4+ (sin @MockBean deprecated)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanServiceIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserServiceClient userServiceClient; // Mock

    @Autowired
    private BookServiceClient bookServiceClient; // Mock

    private UserDto testUser;
    private BookDto testBook;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public UserServiceClient userServiceClient() {
            return mock(UserServiceClient.class);
        }

        @Bean
        @Primary
        public BookServiceClient bookServiceClient() {
            return mock(BookServiceClient.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(userServiceClient, bookServiceClient);

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
    }

    @Test
    void createLoan_ShouldIntegrateWithUserAndBookServices() {
        // Given - Mock responses from other services
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        // When - Create loan
        Loan createdLoan = loanService.createLoan(1L, 1L, "Integration test loan");

        // Then - Verify loan was created and services were called
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
    }

    @Test
    void createLoan_ShouldCalculateCorrectDueDatesForDifferentMemberships() {
        // Test BASIC membership (14 days)
        testUser.setMembershipType("BASIC");
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        Loan basicLoan = loanService.createLoan(1L, 1L);

        assertEquals(LocalDate.now().plusDays(14), basicLoan.getDueDate());

        // Reset mocks for next test
        reset(userServiceClient, bookServiceClient);

        // Test STUDENT membership (21 days)
        testUser.setMembershipType("STUDENT");
        when(userServiceClient.getUserById(2L)).thenReturn(testUser);
        when(userServiceClient.validateUser(2L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        Loan studentLoan = loanService.createLoan(2L, 1L);

        assertEquals(LocalDate.now().plusDays(21), studentLoan.getDueDate());
    }

    @Test
    void returnBook_ShouldIntegrateWithBookService() {
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
    }

    @Test
    void createLoan_ShouldHandleUserServiceFailure() {
        // Given - User service returns error
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("User service unavailable"));

        // When & Then - Should handle gracefully
        assertThrows(Exception.class, () -> loanService.createLoan(1L, 1L));

        // Verify no loan was created
        assertEquals(0, loanRepository.count());

        // Verify book service was not called
        verify(bookServiceClient, never()).updateAvailability(anyLong(), anyInt());
    }

    @Test
    void createLoan_ShouldHandleBookServiceFailure() {
        // Given - Book service returns error
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenThrow(new RuntimeException("Book service unavailable"));

        // When & Then - Should handle gracefully
        assertThrows(Exception.class, () -> loanService.createLoan(1L, 1L));

        // Verify no loan was created
        assertEquals(0, loanRepository.count());
    }

    @Test
    void createLoan_ShouldRespectUserLimits() {
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
    }

    @Test
    void createLoan_ShouldPreventDuplicateBookLoans() {
        // Given - User already has this book
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        // Create existing loan for the same book
        Loan existingLoan = new Loan();
        existingLoan.setUserId(1L);
        existingLoan.setBookId(1L); // Same book ID
        existingLoan.setLoanDate(LocalDate.now().minusDays(1));
        existingLoan.setDueDate(LocalDate.now().plusDays(13));
        existingLoan.setStatus(LoanStatus.ACTIVE);
        loanRepository.save(existingLoan);

        // When & Then - Should reject loan
        assertThrows(IllegalArgumentException.class, () -> loanService.createLoan(1L, 1L));

        // Verify book service was not called to update availability
        verify(bookServiceClient, never()).updateAvailability(anyLong(), anyInt());
    }

    @Test
    void findLoansByUser_ShouldValidateUserExists() {
        // Given
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // When
        var loans = loanService.findLoansByUser(1L);

        // Then
        assertNotNull(loans);
        verify(userServiceClient).getUserById(1L);
    }

    @Test
    void findLoansByUser_ShouldHandleUserNotFound() {
        // Given
        when(userServiceClient.getUserById(999L)).thenThrow(new RuntimeException("User not found"));

        // When & Then
        assertThrows(Exception.class, () -> loanService.findLoansByUser(999L));
    }

    @Test
    void findLoansByBook_ShouldValidateBookExists() {
        // Given
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);

        // When
        var loans = loanService.findLoansByBook(1L);

        // Then
        assertNotNull(loans);
        verify(bookServiceClient).getBookById(1L);
    }

    @Test
    void returnBook_ShouldHandleBookServiceFailureGracefully() {
        // Given - Create a loan first
        Loan loan = new Loan();
        loan.setUserId(1L);
        loan.setBookId(1L);
        loan.setLoanDate(LocalDate.now().minusDays(5));
        loan.setDueDate(LocalDate.now().plusDays(10));
        loan.setStatus(LoanStatus.ACTIVE);
        loan = loanRepository.save(loan);

        // Mock book service to fail
        doThrow(new RuntimeException("Book service unavailable"))
                .when(bookServiceClient).updateAvailability(1L, 1);

        // When - Return the book (should still work despite book service failure)
        Loan returnedLoan = loanService.returnBook(loan.getId());

        // Then - Loan should still be marked as returned
        assertNotNull(returnedLoan);
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
        assertNotNull(returnedLoan.getReturnDate());

        // Verify attempt was made to update book service
        verify(bookServiceClient).updateAvailability(1L, 1);
    }

    @Test
    void createLoan_ShouldWorkWithMinimalUserData() {
        // Given - User with minimal data
        UserDto minimalUser = new UserDto();
        minimalUser.setId(1L);
        minimalUser.setMembershipType("BASIC");
        minimalUser.setIsActive(true);

        when(userServiceClient.getUserById(1L)).thenReturn(minimalUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);

        // When
        Loan createdLoan = loanService.createLoan(1L, 1L);

        // Then
        assertNotNull(createdLoan);
        assertEquals(LocalDate.now().plusDays(14), createdLoan.getDueDate()); // BASIC = 14 days
    }
}