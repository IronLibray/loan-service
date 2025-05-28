package com.ironlibrary.loan_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para LoanController usando MockMvc CORREGIDO
 * Sin @MockBean deprecated, usando @TestConfiguration con @Primary
 */
@WebMvcTest(LoanController.class)
@ActiveProfiles("test")
class LoanControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanService loanService; // Mock via TestConfiguration

    @Autowired
    private ObjectMapper objectMapper;

    private Loan testLoan;

    /**
     * Configuración moderna que reemplaza @MockBean deprecated
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public LoanService loanService() {
            return mock(LoanService.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        reset(loanService);

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
    void getAllLoans_ShouldReturnLoanListAsJson() throws Exception {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findAllLoans()).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getLoanById_ShouldReturnLoanAsJson() throws Exception {
        // Given
        when(loanService.findLoanById(1L)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createQuickLoan_ShouldReturnCreatedLoan() throws Exception {
        // Given
        when(loanService.createLoan(1L, 1L)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(post("/api/loans/quick")
                        .param("userId", "1")
                        .param("bookId", "1"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1));
    }

    @Test
    void createLoan_ShouldReturnCreatedLoanWithStatus201() throws Exception {
        // Given
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Test loan");

        when(loanService.createLoan(1L, 1L, "Test loan")).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1));
    }

    @Test
    void returnBook_ShouldReturnReturnedLoan() throws Exception {
        // Given
        Loan returnedLoan = new Loan();
        returnedLoan.setId(1L);
        returnedLoan.setUserId(1L);
        returnedLoan.setBookId(1L);
        returnedLoan.setStatus(LoanStatus.RETURNED);
        returnedLoan.setReturnDate(LocalDate.now());

        when(loanService.returnBook(1L)).thenReturn(returnedLoan);

        // When & Then
        mockMvc.perform(patch("/api/loans/1/return"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());
    }

    @Test
    void getLoanStatistics_ShouldReturnStatistics() throws Exception {
        // Given
        LoanService.LoanStatistics stats = new LoanService.LoanStatistics(100L, 30L, 10L, 60L);
        when(loanService.getLoanStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/loans/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalLoans").value(100))
                .andExpect(jsonPath("$.activeLoans").value(30))
                .andExpect(jsonPath("$.returnedLoans").value(60));
    }

    @Test
    void deleteLoan_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(loanService).deleteLoan(1L);

        // When & Then
        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void healthCheck_ShouldReturnHealthMessage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/loans/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan Service is running on port 8083"));
    }

    @Test
    void getLoansByUser_ShouldReturnUserLoans() throws Exception {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findLoansByUser(1L)).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans/user/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void getActiveLoansForUser_ShouldReturnActiveLoans() throws Exception {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findActiveLoansForUser(1L)).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans/user/1/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getLoansByBook_ShouldReturnBookLoans() throws Exception {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findLoansByBook(1L)).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans/book/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].bookId").value(1));
    }

    @Test
    void getOverdueLoans_ShouldReturnOverdueLoans() throws Exception {
        // Given
        testLoan.setStatus(LoanStatus.OVERDUE);
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findOverdueLoans()).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans/overdue"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));
    }

    @Test
    void getLoansDueSoon_ShouldReturnLoansDueSoon() throws Exception {
        // Given
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanService.findLoansDueSoon(3)).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans/due-soon?days=3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateLoan_ShouldReturnUpdatedLoan() throws Exception {
        // Given
        Loan updatedLoan = new Loan();
        updatedLoan.setId(1L);
        updatedLoan.setUserId(1L);
        updatedLoan.setBookId(1L);
        updatedLoan.setDueDate(LocalDate.now().plusDays(21));
        updatedLoan.setNotes("Updated notes");
        updatedLoan.setStatus(LoanStatus.ACTIVE);

        when(loanService.updateLoan(eq(1L), any(Loan.class))).thenReturn(updatedLoan);

        // When & Then
        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedLoan)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    @Test
    void extendLoan_ShouldReturnExtendedLoan() throws Exception {
        // Given
        testLoan.setDueDate(LocalDate.now().plusDays(21)); // Extended by 7 days
        when(loanService.extendLoan(1L, 7)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(patch("/api/loans/1/extend?days=7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1));
    }
}
