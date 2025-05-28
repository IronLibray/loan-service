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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@ActiveProfiles("test")
class LoanControllerWorkingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanService loanService; // Mock via TestConfiguration

    @Autowired
    private ObjectMapper objectMapper;

    private Loan validLoan;

    /**
     * Configuración de test que reemplaza @MockBean deprecated
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
        System.out.println("🔧 Configurando test MockMvc");

        // IMPORTANTE: Reset del mock antes de cada test
        reset(loanService);

        // Crear préstamo válido con TODOS los campos necesarios
        validLoan = new Loan();
        validLoan.setId(1L);
        validLoan.setUserId(1L);
        validLoan.setBookId(1L);
        validLoan.setLoanDate(LocalDate.now());           // ✅ Campo requerido
        validLoan.setDueDate(LocalDate.now().plusDays(14)); // ✅ Campo requerido
        validLoan.setReturnDate(null);                    // ✅ Puede ser null
        validLoan.setStatus(LoanStatus.ACTIVE);           // ✅ Campo requerido
        validLoan.setNotes("Préstamo de prueba válido");  // ✅ Campo opcional

        System.out.println("✅ Préstamo de prueba creado: " + validLoan);
    }

    @Test
    void healthCheck_ShouldWork() throws Exception {
        System.out.println("🧪 Test: Health Check");

        mockMvc.perform(get("/api/loans/health"))
                .andDo(print()) // Debug output
                .andExpect(status().isOk())
                .andExpect(content().string("Loan Service is running on port 8083"));

        System.out.println("✅ Health check funcionando");
    }

    @Test
    void getAllLoans_ShouldReturnLoanList() throws Exception {
        System.out.println("🧪 Test: Get All Loans");

        // Given
        List<Loan> loans = Arrays.asList(validLoan);
        when(loanService.findAllLoans()).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans"))
                .andDo(print()) // Debug output
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        // Verify service call
        verify(loanService).findAllLoans();
        System.out.println("✅ Get all loans funcionando");
    }

    @Test
    void getLoanById_ShouldReturnLoan() throws Exception {
        System.out.println("🧪 Test: Get Loan By ID");

        // Given
        when(loanService.findLoanById(1L)).thenReturn(validLoan);

        // When & Then
        mockMvc.perform(get("/api/loans/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.loanDate").exists())
                .andExpect(jsonPath("$.dueDate").exists());

        verify(loanService).findLoanById(1L);
        System.out.println("✅ Get loan by ID funcionando");
    }

    @Test
    void createQuickLoan_ShouldReturnCreatedLoan() throws Exception {
        System.out.println("🧪 Test: Create Quick Loan");

        // Given
        when(loanService.createLoan(1L, 1L)).thenReturn(validLoan);

        // When & Then
        mockMvc.perform(post("/api/loans/quick")
                        .param("userId", "1")
                        .param("bookId", "1"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).createLoan(1L, 1L);
        System.out.println("✅ Create quick loan funcionando");
    }

    @Test
    void createLoan_ShouldReturnCreatedLoan() throws Exception {
        System.out.println("🧪 Test: Create Loan with Body");

        // Given
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Préstamo de prueba");

        when(loanService.createLoan(1L, 1L, "Préstamo de prueba")).thenReturn(validLoan);

        // When & Then
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).createLoan(1L, 1L, "Préstamo de prueba");
        System.out.println("✅ Create loan funcionando");
    }

    @Test
    void returnBook_ShouldReturnReturnedLoan() throws Exception {
        System.out.println("🧪 Test: Return Book");

        // Given - Crear préstamo devuelto CON TODOS LOS CAMPOS
        Loan returnedLoan = new Loan();
        returnedLoan.setId(1L);
        returnedLoan.setUserId(1L);
        returnedLoan.setBookId(1L);
        returnedLoan.setLoanDate(LocalDate.now().minusDays(7));  // ✅ Campo requerido
        returnedLoan.setDueDate(LocalDate.now().plusDays(7));    // ✅ Campo requerido
        returnedLoan.setReturnDate(LocalDate.now());             // ✅ Fecha de devolución
        returnedLoan.setStatus(LoanStatus.RETURNED);             // ✅ Estado devuelto
        returnedLoan.setNotes("Préstamo devuelto");

        when(loanService.returnBook(1L)).thenReturn(returnedLoan);

        // When & Then
        mockMvc.perform(patch("/api/loans/1/return"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists())
                .andExpect(jsonPath("$.loanDate").exists())   // ✅ Verificar campos requeridos
                .andExpect(jsonPath("$.dueDate").exists());   // ✅ Verificar campos requeridos

        verify(loanService).returnBook(1L);
        System.out.println("✅ Return book funcionando");
    }

    @Test
    void extendLoan_ShouldReturnExtendedLoan() throws Exception {
        System.out.println("🧪 Test: Extend Loan");

        // Given - Préstamo con fecha extendida
        Loan extendedLoan = new Loan();
        extendedLoan.setId(1L);
        extendedLoan.setUserId(1L);
        extendedLoan.setBookId(1L);
        extendedLoan.setLoanDate(LocalDate.now().minusDays(7));
        extendedLoan.setDueDate(LocalDate.now().plusDays(21)); // Extendido 7 días más
        extendedLoan.setStatus(LoanStatus.ACTIVE);
        extendedLoan.setNotes("Préstamo extendido");

        when(loanService.extendLoan(1L, 7)).thenReturn(extendedLoan);

        // When & Then
        mockMvc.perform(patch("/api/loans/1/extend")
                        .param("days", "7"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).extendLoan(1L, 7);
        System.out.println("✅ Extend loan funcionando");
    }

    @Test
    void getLoanStatistics_ShouldReturnStats() throws Exception {
        System.out.println("🧪 Test: Get Loan Statistics");

        // Given
        LoanService.LoanStatistics stats = new LoanService.LoanStatistics(100L, 30L, 10L, 60L);
        when(loanService.getLoanStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/loans/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalLoans").value(100))
                .andExpect(jsonPath("$.activeLoans").value(30))
                .andExpect(jsonPath("$.overdueLoans").value(10))
                .andExpect(jsonPath("$.returnedLoans").value(60));

        verify(loanService).getLoanStatistics();
        System.out.println("✅ Get statistics funcionando");
    }

    @Test
    void deleteLoan_ShouldReturnNoContent() throws Exception {
        System.out.println("🧪 Test: Delete Loan");

        // Given
        doNothing().when(loanService).deleteLoan(1L);

        // When & Then
        mockMvc.perform(delete("/api/loans/1"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(loanService).deleteLoan(1L);
        System.out.println("✅ Delete loan funcionando");
    }

    @Test
    void updateLoan_ShouldReturnUpdatedLoan() throws Exception {
        System.out.println("🧪 Test: Update Loan");

        // Given - Préstamo actualizado CON TODOS LOS CAMPOS
        Loan updatedLoan = new Loan();
        updatedLoan.setId(1L);
        updatedLoan.setUserId(1L);
        updatedLoan.setBookId(1L);
        updatedLoan.setLoanDate(LocalDate.now().minusDays(7));
        updatedLoan.setDueDate(LocalDate.now().plusDays(21)); // Actualizado
        updatedLoan.setStatus(LoanStatus.ACTIVE);
        updatedLoan.setNotes("Notas actualizadas");           // Actualizado

        when(loanService.updateLoan(eq(1L), any(Loan.class))).thenReturn(updatedLoan);

        // When & Then
        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedLoan)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.notes").value("Notas actualizadas"));

        verify(loanService).updateLoan(eq(1L), any(Loan.class));
        System.out.println("✅ Update loan funcionando");
    }

    @Test
    void getLoansByUser_ShouldReturnUserLoans() throws Exception {
        System.out.println("🧪 Test: Get Loans By User");

        // Given
        List<Loan> loans = Arrays.asList(validLoan);
        when(loanService.findLoansByUser(1L)).thenReturn(loans);

        // When & Then
        mockMvc.perform(get("/api/loans/user/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(loanService).findLoansByUser(1L);
        System.out.println("✅ Get loans by user funcionando");
    }
}
