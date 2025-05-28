package com.ironlibrary.loan_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
class LoanControllerSimpleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanService loanService;

    @Autowired
    private ObjectMapper objectMapper;

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
        // Reset mock antes de cada test
        reset(loanService);
    }

    @Test
    void healthCheck_ShouldWork() throws Exception {
        // Test más básico posible
        mockMvc.perform(get("/api/loans/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan Service is running on port 8083"));
    }

    @Test
    void getAllLoans_ShouldReturnEmptyList() throws Exception {
        // Given
        when(loanService.findAllLoans()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(loanService).findAllLoans();
    }

    @Test
    void getAllLoans_ShouldReturnLoanList() throws Exception {
        // Given
        Loan testLoan = createTestLoan();
        when(loanService.findAllLoans()).thenReturn(Arrays.asList(testLoan));

        // When & Then
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1));

        verify(loanService).findAllLoans();
    }

    @Test
    void getLoanById_ShouldReturnLoan() throws Exception {
        // Given
        Loan testLoan = createTestLoan();
        when(loanService.findLoanById(1L)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).findLoanById(1L);
    }

    @Test
    void createQuickLoan_ShouldCreateLoan() throws Exception {
        // Given
        Loan testLoan = createTestLoan();
        when(loanService.createLoan(1L, 1L)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(post("/api/loans/quick")
                        .param("userId", "1")
                        .param("bookId", "1"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1));

        verify(loanService).createLoan(1L, 1L);
    }

    @Test
    void createLoan_ShouldCreateLoanWithBody() throws Exception {
        // Given
        Loan testLoan = createTestLoan();
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
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1));

        verify(loanService).createLoan(1L, 1L, "Test loan");
    }

    @Test
    void deleteLoan_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(loanService).deleteLoan(1L);

        // When & Then
        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isNoContent());

        verify(loanService).deleteLoan(1L);
    }

    private Loan createTestLoan() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setUserId(1L);
        loan.setBookId(1L);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setNotes("Test loan");
        return loan;
    }
}
