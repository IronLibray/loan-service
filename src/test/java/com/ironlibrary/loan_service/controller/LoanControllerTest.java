package com.ironlibrary.loan_service.controller;

import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios simples para LoanController
 */
@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    private Loan testLoan;

    @BeforeEach
    void setUp() {
        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setUserId(1L);
        testLoan.setBookId(1L);
        testLoan.setLoanDate(LocalDate.now());
        testLoan.setDueDate(LocalDate.now().plusDays(14));
        testLoan.setStatus(LoanStatus.ACTIVE);
        testLoan.setNotes("Préstamo de prueba");
    }

    @Test
    void getAllLoans_ShouldReturnLoanList() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findAllLoans()).thenReturn(loans);

        // When
        ResponseEntity<List<Loan>> response = loanController.getAllLoans();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testLoan.getId(), response.getBody().get(0).getId());
        verify(loanService).findAllLoans();
    }

    @Test
    void getLoanById_ShouldReturnLoan() {
        // Given
        when(loanService.findLoanById(1L)).thenReturn(testLoan);

        // When
        ResponseEntity<Loan> response = loanController.getLoanById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testLoan.getId(), response.getBody().getId());
        assertEquals(testLoan.getUserId(), response.getBody().getUserId());
        verify(loanService).findLoanById(1L);
    }

    @Test
    void getLoansByUser_ShouldReturnUserLoans() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findLoansByUser(1L)).thenReturn(loans);

        // When
        ResponseEntity<List<Loan>> response = loanController.getLoansByUser(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getUserId());
        verify(loanService).findLoansByUser(1L);
    }

    @Test
    void getActiveLoansForUser_ShouldReturnActiveLoans() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findActiveLoansForUser(1L)).thenReturn(loans);

        // When
        ResponseEntity<List<Loan>> response = loanController.getActiveLoansForUser(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(LoanStatus.ACTIVE, response.getBody().get(0).getStatus());
        verify(loanService).findActiveLoansForUser(1L);
    }

    @Test
    void getLoansByBook_ShouldReturnBookLoans() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findLoansByBook(1L)).thenReturn(loans);

        // When
        ResponseEntity<List<Loan>> response = loanController.getLoansByBook(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getBookId());
        verify(loanService).findLoansByBook(1L);
    }

    @Test
    void getOverdueLoans_ShouldReturnOverdueLoans() {
        // Given
        testLoan.setStatus(LoanStatus.OVERDUE);
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findOverdueLoans()).thenReturn(loans);

        // When
        ResponseEntity<List<Loan>> response = loanController.getOverdueLoans();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(LoanStatus.OVERDUE, response.getBody().get(0).getStatus());
        verify(loanService).findOverdueLoans();
    }

    @Test
    void getLoansDueSoon_ShouldReturnLoansDueSoon() {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findLoansDueSoon(3)).thenReturn(loans);

        // When
        ResponseEntity<List<Loan>> response = loanController.getLoansDueSoon(3);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(loanService).findLoansDueSoon(3);
    }

    @Test
    void getLoanStatistics_ShouldReturnStats() {
        // Given
        LoanService.LoanStatistics stats = new LoanService.LoanStatistics(100L, 30L, 10L, 60L);
        when(loanService.getLoanStatistics()).thenReturn(stats);

        // When
        ResponseEntity<LoanService.LoanStatistics> response = loanController.getLoanStatistics();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().totalLoans);
        assertEquals(30L, response.getBody().activeLoans);
        verify(loanService).getLoanStatistics();
    }

    @Test
    void createLoan_ShouldReturnCreatedLoan() {
        // Given
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest(1L, 1L, "Test notes");
        when(loanService.createLoan(1L, 1L, "Test notes")).thenReturn(testLoan);

        // When
        ResponseEntity<Loan> response = loanController.createLoan(request);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testLoan.getId(), response.getBody().getId());
        verify(loanService).createLoan(1L, 1L, "Test notes");
    }

    @Test
    void createQuickLoan_ShouldReturnCreatedLoan() {
        // Given
        when(loanService.createLoan(1L, 1L)).thenReturn(testLoan);

        // When
        ResponseEntity<Loan> response = loanController.createQuickLoan(1L, 1L);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testLoan.getId(), response.getBody().getId());
        verify(loanService).createLoan(1L, 1L);
    }

    @Test
    void updateLoan_ShouldReturnUpdatedLoan() {
        // Given
        Loan updatedLoan = new Loan();
        updatedLoan.setId(1L);
        updatedLoan.setDueDate(LocalDate.now().plusDays(21));
        updatedLoan.setNotes("Notas actualizadas");

        when(loanService.updateLoan(eq(1L), any(Loan.class))).thenReturn(updatedLoan);

        // When
        ResponseEntity<Loan> response = loanController.updateLoan(1L, updatedLoan);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(updatedLoan.getDueDate(), response.getBody().getDueDate());
        verify(loanService).updateLoan(eq(1L), any(Loan.class));
    }

    @Test
    void returnBook_ShouldReturnReturnedLoan() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        testLoan.setReturnDate(LocalDate.now());
        when(loanService.returnBook(1L)).thenReturn(testLoan);

        // When
        ResponseEntity<Loan> response = loanController.returnBook(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(LoanStatus.RETURNED, response.getBody().getStatus());
        assertNotNull(response.getBody().getReturnDate());
        verify(loanService).returnBook(1L);
    }

    @Test
    void extendLoan_ShouldReturnExtendedLoan() {
        // Given
        testLoan.setDueDate(LocalDate.now().plusDays(21)); // Extended by 7 days
        when(loanService.extendLoan(1L, 7)).thenReturn(testLoan);

        // When
        ResponseEntity<Loan> response = loanController.extendLoan(1L, 7);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testLoan.getDueDate(), response.getBody().getDueDate());
        verify(loanService).extendLoan(1L, 7);
    }

    @Test
    void deleteLoan_ShouldReturnNoContent() {
        // Given
        doNothing().when(loanService).deleteLoan(1L);

        // When
        ResponseEntity<Void> response = loanController.deleteLoan(1L);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(loanService).deleteLoan(1L);
    }

    @Test
    void healthCheck_ShouldReturnOk() {
        // When
        ResponseEntity<String> response = loanController.healthCheck();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Loan Service is running on port 8083", response.getBody());
    }

    @Test
    void createLoanRequest_ShouldWorkWithConstructors() {
        // Test default constructor
        LoanController.CreateLoanRequest request1 = new LoanController.CreateLoanRequest();
        assertNotNull(request1);

        // Test parameterized constructor
        LoanController.CreateLoanRequest request2 = new LoanController.CreateLoanRequest(1L, 2L, "Test");
        assertEquals(1L, request2.getUserId());
        assertEquals(2L, request2.getBookId());
        assertEquals("Test", request2.getNotes());

        // Test setters
        request1.setUserId(3L);
        request1.setBookId(4L);
        request1.setNotes("New notes");

        assertEquals(3L, request1.getUserId());
        assertEquals(4L, request1.getBookId());
        assertEquals("New notes", request1.getNotes());
    }
}
