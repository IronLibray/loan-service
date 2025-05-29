package com.ironlibrary.loan_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.openfeign.client.config.default.connect-timeout=1000",
        "spring.cloud.openfeign.client.config.default.read-timeout=1000",
        "eureka.client.enabled=false"
})
@Transactional
class LoanControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        System.out.println("🧪 === Controller Integration Test - Setup ===");

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Limpiar base de datos
        loanRepository.deleteAll();

        System.out.println("✅ MockMvc configurado + BD limpia");
    }

    @Test
    void healthCheck_ShouldAlwaysWork() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/health");

        // When & Then
        mockMvc.perform(get("/api/loans/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("Loan Service is running on port 8083"));

        System.out.println("✅ GET /api/loans/health funciona correctamente");
    }

    @Test
    void getAllLoans_ShouldReturnAllFromDatabase() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans");

        // Given - Crear préstamos directamente en BD
        Loan loan1 = createTestLoanInDB(1L, 101L, "Préstamo 1", LoanStatus.ACTIVE);
        Loan loan2 = createTestLoanInDB(2L, 102L, "Préstamo 2", LoanStatus.RETURNED);
        Loan loan3 = createTestLoanInDB(1L, 103L, "Préstamo 3", LoanStatus.OVERDUE);

        // When & Then
        mockMvc.perform(get("/api/loans"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        loan1.getId().intValue(),
                        loan2.getId().intValue(),
                        loan3.getId().intValue())))
                .andExpect(jsonPath("$[*].userId", containsInAnyOrder(1, 2, 1)))
                .andExpect(jsonPath("$[*].status", containsInAnyOrder("ACTIVE", "RETURNED", "OVERDUE")));

        System.out.println("✅ GET /api/loans funciona correctamente");
    }

    @Test
    void getLoanById_ShouldReturnSpecificLoan() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/{id}");

        // Given - Crear préstamo en BD
        Loan loan = createTestLoanInDB(1L, 101L, "Préstamo específico", LoanStatus.ACTIVE);

        // When & Then
        mockMvc.perform(get("/api/loans/" + loan.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(101))
                .andExpect(jsonPath("$.notes").value("Préstamo específico"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.loanDate").exists())
                .andExpect(jsonPath("$.dueDate").exists());

        System.out.println("✅ GET /api/loans/{id} funciona correctamente");
    }

    @Test
    void getLoanById_NotFound_ShouldReturn404() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/{id} - Not Found");

        // When & Then - Buscar préstamo que no existe
        mockMvc.perform(get("/api/loans/999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Préstamo no encontrado con ID: 999"))
                .andExpect(jsonPath("$.timestamp").exists());

        System.out.println("✅ Manejo de error 404 funciona correctamente");
    }

    @Test
    void getActiveLoansForUser_ShouldFilterByUserAndStatus() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/user/{userId}/active");

        // Given - Crear préstamos con diferentes estados para mismo usuario
        createTestLoanInDB(1L, 101L, "Activo", LoanStatus.ACTIVE);
        createTestLoanInDB(1L, 102L, "Devuelto", LoanStatus.RETURNED);
        createTestLoanInDB(1L, 103L, "Vencido pero activo", LoanStatus.OVERDUE);

        // When & Then
        mockMvc.perform(get("/api/loans/user/1/active"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1)) // Solo el ACTIVE
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].bookId").value(101));

        System.out.println("✅ GET /api/loans/user/{userId}/active funciona correctamente");
    }

    @Test
    void updateLoan_ShouldPersistChanges() throws Exception {
        System.out.println("🧪 Controller Integration: PUT /api/loans/{id}");

        // Given - Crear préstamo
        Loan loan = createTestLoanInDB(1L, 101L, "Original", LoanStatus.ACTIVE);

        // Datos de actualización
        Loan updateData = new Loan();
        updateData.setDueDate(LocalDate.now().plusDays(45));
        updateData.setNotes("Notas actualizadas por HTTP");

        // When & Then
        mockMvc.perform(put("/api/loans/" + loan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.notes").value("Notas actualizadas por HTTP"));

        // Verificar en base de datos
        Loan updatedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertEquals("Notas actualizadas por HTTP", updatedLoan.getNotes());
        assertEquals(LocalDate.now().plusDays(45), updatedLoan.getDueDate());

        System.out.println("✅ PUT /api/loans/{id} funciona correctamente");
    }

    @Test
    void extendLoan_ShouldUpdateDueDateInDatabase() throws Exception {
        System.out.println("🧪 Controller Integration: PATCH /api/loans/{id}/extend");

        // Given - Crear préstamo activo
        Loan loan = createTestLoanInDB(1L, 101L, "Para extender", LoanStatus.ACTIVE);
        LocalDate originalDueDate = loan.getDueDate();

        // When & Then
        mockMvc.perform(patch("/api/loans/" + loan.getId() + "/extend")
                        .param("days", "15"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verificar cambios en base de datos
        Loan extendedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertEquals(originalDueDate.plusDays(15), extendedLoan.getDueDate());
        assertEquals(LoanStatus.ACTIVE, extendedLoan.getStatus());

        System.out.println("✅ PATCH /api/loans/{id}/extend funciona correctamente");
    }

    @Test
    void getLoanStatistics_ShouldReturnRealStatistics() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/stats");

        // Given - Crear préstamos con diferentes estados
        createTestLoanInDB(1L, 101L, "Activo 1", LoanStatus.ACTIVE);
        createTestLoanInDB(2L, 102L, "Activo 2", LoanStatus.ACTIVE);
        createTestLoanInDB(3L, 103L, "Devuelto", LoanStatus.RETURNED);
        createTestLoanInDB(4L, 104L, "Vencido", LoanStatus.OVERDUE);

        // When & Then
        mockMvc.perform(get("/api/loans/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalLoans").value(4))
                .andExpect(jsonPath("$.activeLoans").value(2))
                .andExpect(jsonPath("$.returnedLoans").value(1))
                .andExpect(jsonPath("$.overdueLoans").value(1));

        System.out.println("✅ GET /api/loans/stats funciona correctamente");
    }

    @Test
    void getOverdueLoans_ShouldReturnAndUpdateOverdueLoans() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/overdue");

        // Given - Crear préstamo vencido
        Loan overdueLoan = createTestLoanInDB(1L, 101L, "Vencido", LoanStatus.ACTIVE);
        overdueLoan.setDueDate(LocalDate.now().minusDays(8)); // Vencido hace 8 días
        loanRepository.save(overdueLoan);

        // Crear préstamo no vencido para contraste
        createTestLoanInDB(2L, 102L, "No vencido", LoanStatus.ACTIVE);

        // When & Then
        mockMvc.perform(get("/api/loans/overdue"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(overdueLoan.getId()))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));

        // Verificar que se actualizó el estado en BD
        Loan updatedLoan = loanRepository.findById(overdueLoan.getId()).orElseThrow();
        assertEquals(LoanStatus.OVERDUE, updatedLoan.getStatus());

        System.out.println("✅ GET /api/loans/overdue funciona correctamente");
    }

    @Test
    void getLoansDueSoon_ShouldReturnUpcomingDueDates() throws Exception {
        System.out.println("🧪 Controller Integration: GET /api/loans/due-soon");

        // Given - Crear préstamos con diferentes fechas de vencimiento
        Loan dueSoonLoan = createTestLoanInDB(1L, 101L, "Vence pronto", LoanStatus.ACTIVE);
        dueSoonLoan.setDueDate(LocalDate.now().plusDays(2)); // Vence en 2 días
        loanRepository.save(dueSoonLoan);

        Loan dueLateLoan = createTestLoanInDB(2L, 102L, "Vence tarde", LoanStatus.ACTIVE);
        dueLateLoan.setDueDate(LocalDate.now().plusDays(10)); // Vence en 10 días
        loanRepository.save(dueLateLoan);

        // When & Then - Buscar préstamos que vencen en 5 días
        mockMvc.perform(get("/api/loans/due-soon?days=5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1)) // Solo dueSoonLoan
                .andExpect(jsonPath("$[0].id").value(dueSoonLoan.getId()))
                .andExpect(jsonPath("$[0].notes").value("Vence pronto"));

        System.out.println("✅ GET /api/loans/due-soon funciona correctamente");
    }

    @Test
    void errorHandling_ShouldReturnProperErrorResponse() throws Exception {
        System.out.println("🧪 Controller Integration: Manejo de errores");

        // Error 400 - Datos inválidos para extensión
        Loan loan = createTestLoanInDB(1L, 101L, "Para error", LoanStatus.ACTIVE);

        mockMvc.perform(patch("/api/loans/" + loan.getId() + "/extend?days=50")) // > 30 días
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        System.out.println("✅ Manejo de errores HTTP funciona correctamente");
    }

    // ✅ HELPER METHODS

    private Loan createTestLoanInDB(Long userId, Long bookId, String notes, LoanStatus status) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setLoanDate(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(23));
        loan.setStatus(status);
        loan.setNotes(notes);

        if (status == LoanStatus.RETURNED) {
            loan.setReturnDate(LocalDate.now().minusDays(1));
        }

        return loanRepository.save(loan);
    }
}