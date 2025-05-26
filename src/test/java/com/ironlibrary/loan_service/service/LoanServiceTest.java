package com.ironlibrary.loan_service.service;

import com.ironlibrary.loan_service.client.BookServiceClient;
import com.ironlibrary.loan_service.client.UserServiceClient;
import com.ironlibrary.loan_service.client.dto.BookDto;
import com.ironlibrary.loan_service.client.dto.UserDto;
import com.ironlibrary.loan_service.exception.BookNotAvailableException;
import com.ironlibrary.loan_service.exception.LoanAlreadyReturnedException;
import com.ironlibrary.loan_service.exception.LoanNotFoundException;
import com.ironlibrary.loan_service.exception.UserNotValidException;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para LoanService
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookServiceClient bookServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private LoanService loanService;

    private Loan testLoan;
    private UserDto testUser;
    private BookDto testBook;

    @BeforeEach
    void setUp() {
        // Setup test loan
        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setUserId(1L);
        testLoan.setBookId(1L);
        testLoan.setLoanDate(LocalDate.now());
        testLoan.setDueDate(LocalDate.now().plusDays(14));
        testLoan.setStatus(LoanStatus.ACTIVE);
        testLoan.setNotes("Préstamo de prueba");

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
    void createLoan_ShouldCreateLoan_WhenValidData() {
        // Given
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        when(loanRepository.countActiveLoansForUser(1L)).thenReturn(0L);
        when(loanRepository.hasActiveLoanForBook(1L, 1L)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.createLoan(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getBookId());
        verify(bookServiceClient).updateAvailability(1L, -1);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void createLoan_ShouldThrowException_WhenUserNotValid() {
        // Given
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(false); // Usuario no válido

        // When & Then
        UserNotValidException exception = assertThrows(
                UserNotValidException.class,
                () -> loanService.createLoan(1L, 1L)
        );

        // Usar contains para ser más flexible con el mensaje
        assertTrue(exception.getMessage().contains("no puede pedir libros prestados"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_ShouldThrowException_WhenBookNotAvailable() {
        // Given
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(false); // Libro no disponible

        // When & Then
        BookNotAvailableException exception = assertThrows(
                BookNotAvailableException.class,
                () -> loanService.createLoan(1L, 1L)
        );

        // Usar contains para ser más flexible con el mensaje
        assertTrue(exception.getMessage().contains("no está disponible"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_ShouldThrowException_WhenUserReachesLimit() {
        // Given
        testUser.setMembershipType("BASIC"); // 3 books max
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        when(loanRepository.countActiveLoansForUser(1L)).thenReturn(3L); // Already at limit

        // When & Then
        UserNotValidException exception = assertThrows(
                UserNotValidException.class,
                () -> loanService.createLoan(1L, 1L)
        );

        assertTrue(exception.getMessage().contains("límite de libros prestados"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_ShouldThrowException_WhenUserAlreadyHasBook() {
        // Given
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        when(loanRepository.countActiveLoansForUser(1L)).thenReturn(0L);
        when(loanRepository.hasActiveLoanForBook(1L, 1L)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.createLoan(1L, 1L)
        );

        assertTrue(exception.getMessage().contains("ya tiene este libro prestado"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void findAllLoans_ShouldReturnAllLoans() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanRepository.findAll()).thenReturn(loans);

        // When
        List<Loan> result = loanService.findAllLoans();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testLoan.getId(), result.get(0).getId());
        verify(loanRepository).findAll();
    }

    @Test
    void findLoanById_ShouldReturnLoan_WhenLoanExists() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When
        Loan result = loanService.findLoanById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testLoan.getId(), result.getId());
        verify(loanRepository).findById(1L);
    }

    @Test
    void findLoanById_ShouldThrowException_WhenLoanNotExists() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        LoanNotFoundException exception = assertThrows(
                LoanNotFoundException.class,
                () -> loanService.findLoanById(1L)
        );

        assertEquals("Préstamo no encontrado con ID: 1", exception.getMessage());
        verify(loanRepository).findById(1L);
    }

    @Test
    void findLoansByUser_ShouldReturnUserLoans() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(loanRepository.findByUserId(1L)).thenReturn(loans);

        // When
        List<Loan> result = loanService.findLoansByUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userServiceClient).getUserById(1L);
        verify(loanRepository).findByUserId(1L);
    }

    @Test
    void findLoansByUser_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("User not found"));

        // When & Then
        UserNotValidException exception = assertThrows(
                UserNotValidException.class,
                () -> loanService.findLoansByUser(1L)
        );

        assertTrue(exception.getMessage().contains("Usuario no encontrado: 1"));
        verify(userServiceClient).getUserById(1L);
        verify(loanRepository, never()).findByUserId(any());
    }

    @Test
    void findActiveLoansForUser_ShouldReturnActiveLoans() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanRepository.findByUserIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(loans);

        // When
        List<Loan> result = loanService.findActiveLoansForUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(loanRepository).findByUserIdAndStatus(1L, LoanStatus.ACTIVE);
    }

    @Test
    void returnBook_ShouldReturnBook_WhenLoanIsActive() {
        // Given
        testLoan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.returnBook(1L);

        // Then
        assertEquals(LoanStatus.RETURNED, testLoan.getStatus());
        assertNotNull(testLoan.getReturnDate());
        verify(bookServiceClient).updateAvailability(1L, 1);
        verify(loanRepository).save(testLoan);
    }

    @Test
    void returnBook_ShouldThrowException_WhenLoanAlreadyReturned() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        LoanAlreadyReturnedException exception = assertThrows(
                LoanAlreadyReturnedException.class,
                () -> loanService.returnBook(1L)
        );

        assertTrue(exception.getMessage().contains("ya fue devuelto"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void updateLoan_ShouldUpdateLoan_WhenValidData() {
        // Given
        Loan updateData = new Loan();
        updateData.setDueDate(LocalDate.now().plusDays(21));
        updateData.setNotes("Notas actualizadas");

        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.updateLoan(1L, updateData);

        // Then
        assertEquals(updateData.getDueDate(), testLoan.getDueDate());
        assertEquals(updateData.getNotes(), testLoan.getNotes());
        verify(loanRepository).save(testLoan);
    }

    @Test
    void deleteLoan_ShouldDeleteLoan_WhenLoanNotActive() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When
        loanService.deleteLoan(1L);

        // Then
        verify(loanRepository).delete(testLoan);
    }

    @Test
    void deleteLoan_ShouldThrowException_WhenLoanIsActive() {
        // Given
        testLoan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.deleteLoan(1L)
        );

        assertTrue(exception.getMessage().contains("No se puede eliminar un préstamo activo"));
        verify(loanRepository, never()).delete(any());
    }

    @Test
    void findOverdueLoans_ShouldReturnAndUpdateOverdueLoans() {
        // Given
        testLoan.setDueDate(LocalDate.now().minusDays(5)); // Vencido
        testLoan.setStatus(LoanStatus.ACTIVE);
        List<Loan> overdueLoans = Arrays.asList(testLoan);
        when(loanRepository.findOverdueLoans()).thenReturn(overdueLoans);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        List<Loan> result = loanService.findOverdueLoans();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LoanStatus.OVERDUE, testLoan.getStatus());
        verify(loanRepository).save(testLoan);
    }

    @Test
    void extendLoan_ShouldExtendLoan_WhenValidData() {
        // Given
        testLoan.setStatus(LoanStatus.ACTIVE);
        LocalDate originalDueDate = testLoan.getDueDate();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.extendLoan(1L, 7);

        // Then
        assertEquals(originalDueDate.plusDays(7), testLoan.getDueDate());
        verify(loanRepository).save(testLoan);
    }

    @Test
    void extendLoan_ShouldThrowException_WhenLoanNotActive() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.extendLoan(1L, 7)
        );

        assertTrue(exception.getMessage().contains("Solo se pueden extender préstamos activos"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void extendLoan_ShouldThrowException_WhenInvalidDays() {
        // Given
        testLoan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.extendLoan(1L, 35) // Más de 30 días
        );

        assertTrue(exception.getMessage().contains("deben estar entre 1 y 30"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void getLoanStatistics_ShouldReturnCorrectStats() {
        // Given
        when(loanRepository.count()).thenReturn(100L);
        when(loanRepository.getLoanStatistics()).thenReturn(Arrays.asList(
                new Object[]{LoanStatus.ACTIVE, 30L},
                new Object[]{LoanStatus.RETURNED, 60L},
                new Object[]{LoanStatus.OVERDUE, 10L}
        ));

        // When
        LoanService.LoanStatistics result = loanService.getLoanStatistics();

        // Then
        assertEquals(100L, result.totalLoans);
        assertEquals(30L, result.activeLoans);
        assertEquals(10L, result.overdueLoans);
        assertEquals(60L, result.returnedLoans);
    }
}
