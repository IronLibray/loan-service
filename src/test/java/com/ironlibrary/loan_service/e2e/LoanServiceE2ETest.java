package com.ironlibrary.loan_service.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironlibrary.loan_service.controller.LoanController;
import com.ironlibrary.loan_service.exception.GlobalExceptionHandler;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test E2E Moderno SIN @MockBean (deprecated)
 *
 * ESTRATEGIA MODERNA 2025:
 * - @ExtendWith(MockitoExtension.class) - Mockito puro
 * - @Mock - Mocks modernos sin Spring Boot
 * - MockMvc.standaloneSetup() - Configuración manual
 * - Sin dependencias de Spring Boot Testing deprecated
 * - Testing puro y rápido
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LoanServiceE2ETest {

    // ✅ MOCKITO PURO - Sin @MockBean deprecated
    @Mock
    private LoanService loanService;

    // Configuración manual
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LoanController loanController;

    // Datos de prueba
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        System.out.println("🚀 E2E Test Moderno (Sin @MockBean) - Configuración iniciada");

        // ✅ CONFIGURACIÓN MANUAL COMPLETA
        setupController();
        setupMockMvc();
        setupObjectMapper();
        setupTestData();
        resetMocks();

        System.out.println("✅ E2E Test Moderno - Configuración completada");
    }

    private void setupController() {
        // Crear controller manualmente con el mock
        loanController = new LoanController(loanService);
        System.out.println("🔧 Controller creado manualmente con mock service");
    }

    private void setupMockMvc() {
        // Configurar MockMvc standalone (sin Spring Context)
        mockMvc = MockMvcBuilders.standaloneSetup(loanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        System.out.println("🔧 MockMvc configurado en modo standalone");
    }

    private void setupObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Para soporte de LocalDate
        System.out.println("🔧 ObjectMapper configurado");
    }

    private void setupTestData() {
        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setUserId(1L);
        testLoan.setBookId(1L);
        testLoan.setLoanDate(LocalDate.now());
        testLoan.setDueDate(LocalDate.now().plusDays(30));
        testLoan.setStatus(LoanStatus.ACTIVE);
        testLoan.setNotes("Préstamo E2E Moderno");
        System.out.println("📊 Datos de prueba configurados");
    }

    private void resetMocks() {
        // Reset explícito del mock (opcional, Mockito lo hace automáticamente)
        reset(loanService);
        System.out.println("🧹 Mocks reseteados");
    }

    @Test
    void healthCheck_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Health Check");

        mockMvc.perform(get("/api/loans/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Loan Service is running on port 8083"));

        // Health check no interactúa con el service
        verifyNoInteractions(loanService);

        System.out.println("✅ Health check funcionando");
    }

    @Test
    void createLoan_FullFlow_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Crear préstamo - Flujo completo");

        // Given - Configurar comportamiento del mock
        when(loanService.createLoan(1L, 1L, "Préstamo E2E Moderno")).thenReturn(testLoan);

        // When - Realizar request
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Préstamo E2E Moderno");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.notes").value("Préstamo E2E Moderno"));

        // Then - Verificar interacciones
        verify(loanService).createLoan(1L, 1L, "Préstamo E2E Moderno");
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Crear préstamo - Flujo completo funcionando");
    }

    @Test
    void createQuickLoan_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Crear préstamo rápido");

        // Given
        when(loanService.createLoan(1L, 1L)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(post("/api/loans/quick")
                        .param("userId", "1")
                        .param("bookId", "1"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).createLoan(1L, 1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Crear préstamo rápido funcionando");
    }

    @Test
    void getAllLoans_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Obtener todos los préstamos");

        // Given
        Loan loan1 = createLoan(1L, 1L, 1L, "Préstamo 1", LoanStatus.ACTIVE);
        Loan loan2 = createLoan(2L, 2L, 2L, "Préstamo 2", LoanStatus.RETURNED);

        when(loanService.findAllLoans()).thenReturn(Arrays.asList(loan1, loan2));

        // When & Then
        mockMvc.perform(get("/api/loans"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].notes").value("Préstamo 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].notes").value("Préstamo 2"));

        verify(loanService).findAllLoans();
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Obtener todos los préstamos funcionando");
    }

    @Test
    void getLoanById_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Obtener préstamo por ID");

        // Given
        when(loanService.findLoanById(1L)).thenReturn(testLoan);

        // When & Then
        mockMvc.perform(get("/api/loans/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).findLoanById(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Obtener préstamo por ID funcionando");
    }

    @Test
    void getUserLoans_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Obtener préstamos por usuario");

        // Given
        when(loanService.findLoansByUser(1L)).thenReturn(Arrays.asList(testLoan));

        // When & Then
        mockMvc.perform(get("/api/loans/user/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(loanService).findLoansByUser(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Obtener préstamos por usuario funcionando");
    }

    @Test
    void getActiveLoansForUser_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Obtener préstamos activos por usuario");

        // Given
        when(loanService.findActiveLoansForUser(1L)).thenReturn(Arrays.asList(testLoan));

        // When & Then
        mockMvc.perform(get("/api/loans/user/1/active"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(loanService).findActiveLoansForUser(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Obtener préstamos activos por usuario funcionando");
    }

    @Test
    void returnBook_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Devolver libro");

        // Given - Préstamo devuelto
        Loan returnedLoan = createLoan(1L, 1L, 1L, "Libro devuelto", LoanStatus.RETURNED);
        returnedLoan.setReturnDate(LocalDate.now());

        when(loanService.returnBook(1L)).thenReturn(returnedLoan);

        // When & Then
        mockMvc.perform(patch("/api/loans/1/return"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());

        verify(loanService).returnBook(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Devolver libro funcionando");
    }

    @Test
    void extendLoan_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Extender préstamo");

        // Given - Préstamo extendido
        Loan extendedLoan = createLoan(1L, 1L, 1L, "Préstamo extendido", LoanStatus.ACTIVE);
        extendedLoan.setDueDate(LocalDate.now().plusDays(37)); // Extendido 7 días

        when(loanService.extendLoan(1L, 7)).thenReturn(extendedLoan);

        // When & Then
        mockMvc.perform(patch("/api/loans/1/extend?days=7"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).extendLoan(1L, 7);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Extender préstamo funcionando");
    }

    @Test
    void updateLoan_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Actualizar préstamo");

        // Given
        Loan updatedLoan = createLoan(1L, 1L, 1L, "Notas actualizadas", LoanStatus.ACTIVE);
        updatedLoan.setDueDate(LocalDate.now().plusDays(21));

        when(loanService.updateLoan(eq(1L), any(Loan.class))).thenReturn(updatedLoan);

        // When & Then
        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedLoan)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.notes").value("Notas actualizadas"));

        verify(loanService).updateLoan(eq(1L), any(Loan.class));
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Actualizar préstamo funcionando");
    }

    @Test
    void deleteLoan_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Eliminar préstamo");

        // Given
        doNothing().when(loanService).deleteLoan(1L);

        // When & Then
        mockMvc.perform(delete("/api/loans/1"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(loanService).deleteLoan(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Eliminar préstamo funcionando");
    }

    @Test
    void getLoanStatistics_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Obtener estadísticas");

        // Given
        LoanService.LoanStatistics stats = new LoanService.LoanStatistics(100L, 30L, 10L, 60L);
        when(loanService.getLoanStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/loans/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans").value(100))
                .andExpect(jsonPath("$.activeLoans").value(30))
                .andExpect(jsonPath("$.overdueLoans").value(10))
                .andExpect(jsonPath("$.returnedLoans").value(60));

        verify(loanService).getLoanStatistics();
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Obtener estadísticas funcionando");
    }

    @Test
    void createLoan_WithServiceException_ShouldHandleGracefully() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Manejar errores del servicio");

        // Given - Simular excepción del servicio
        when(loanService.createLoan(anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("Usuario no válido"));

        // When & Then
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(999L); // Usuario inválido
        request.setBookId(1L);
        request.setNotes("Error test");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuario no válido"));

        verify(loanService).createLoan(999L, 1L, "Error test");
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Manejo de errores del servicio funcionando");
    }

    @Test
    void wholeLoanLifecycle_EndToEnd_ShouldWork() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Ciclo de vida completo del préstamo");

        // Step 1: Crear préstamo
        when(loanService.createLoan(1L, 1L, "Ciclo completo")).thenReturn(testLoan);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoanController.CreateLoanRequest(1L, 1L, "Ciclo completo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Step 2: Extender préstamo
        Loan extendedLoan = createLoan(1L, 1L, 1L, "Ciclo completo", LoanStatus.ACTIVE);
        extendedLoan.setDueDate(LocalDate.now().plusDays(37));
        when(loanService.extendLoan(1L, 7)).thenReturn(extendedLoan);

        mockMvc.perform(patch("/api/loans/1/extend?days=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Step 3: Devolver libro
        Loan returnedLoan = createLoan(1L, 1L, 1L, "Ciclo completo", LoanStatus.RETURNED);
        returnedLoan.setReturnDate(LocalDate.now());
        when(loanService.returnBook(1L)).thenReturn(returnedLoan);

        mockMvc.perform(patch("/api/loans/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        // Verificar todas las interacciones en orden
        verify(loanService).createLoan(1L, 1L, "Ciclo completo");
        verify(loanService).extendLoan(1L, 7);
        verify(loanService).returnBook(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Ciclo de vida completo del préstamo funcionando");
    }

    @Test
    void multipleOperations_ShouldWorkInSequence() throws Exception {
        System.out.println("🧪 E2E Moderno Test: Múltiples operaciones en secuencia");

        // Configurar mocks para múltiples operaciones
        when(loanService.findAllLoans()).thenReturn(Arrays.asList(testLoan));
        when(loanService.findLoanById(1L)).thenReturn(testLoan);
        when(loanService.findLoansByUser(1L)).thenReturn(Arrays.asList(testLoan));

        // Ejecutar múltiples requests
        mockMvc.perform(get("/api/loans")).andExpect(status().isOk());
        mockMvc.perform(get("/api/loans/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/loans/user/1")).andExpect(status().isOk());

        // Verificar todas las llamadas
        verify(loanService).findAllLoans();
        verify(loanService).findLoanById(1L);
        verify(loanService).findLoansByUser(1L);
        verifyNoMoreInteractions(loanService);

        System.out.println("✅ Múltiples operaciones en secuencia funcionando");
    }

    // ✅ HELPER METHODS
    private Loan createLoan(Long id, Long userId, Long bookId, String notes, LoanStatus status) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(30));
        loan.setStatus(status);
        loan.setNotes(notes);
        return loan;
    }
}