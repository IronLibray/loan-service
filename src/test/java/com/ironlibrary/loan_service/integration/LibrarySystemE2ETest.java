package com.ironlibrary.loan_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironlibrary.loan_service.client.BookServiceClient;
import com.ironlibrary.loan_service.client.UserServiceClient;
import com.ironlibrary.loan_service.client.dto.BookDto;
import com.ironlibrary.loan_service.client.dto.UserDto;
import com.ironlibrary.loan_service.controller.LoanController;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;
import com.ironlibrary.loan_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test End-to-End SIMPLE sin dependencias de Spring Cloud
 * Usa @MockBean que es la forma más directa
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.cloud.loadbalancer.enabled=false"
})
class LibrarySystemE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private BookServiceClient bookServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUser;
    private BookDto testBook;

    @BeforeEach
    void setUp() {
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
        testBook.setTitle("Cien años de soledad");
        testBook.setAuthor("Gabriel García Márquez");
        testBook.setAvailableCopies(5);
    }

    @Test
    void healthCheck_ShouldWork() throws Exception {
        // Test más básico para verificar que todo funciona
        mockMvc.perform(get("/api/loans/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan Service is running on port 8083"));

        System.out.println("✅ Health check funciona correctamente");
    }

    @Test
    void completeLibraryWorkflow_ShouldWorkEndToEnd() throws Exception {
        System.out.println("🧪 Iniciando test E2E completo");

        // STEP 1: Setup mocks for successful loan
        setupMocksForSuccessfulLoan();

        // STEP 2: Create a loan via API
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("E2E test loan");

        System.out.println("📝 Creando préstamo...");
        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.notes").value("E2E test loan"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // STEP 3: Parse response to get loan ID
        Loan createdLoan = objectMapper.readValue(response, Loan.class);
        Long loanId = createdLoan.getId();
        assertNotNull(loanId);
        System.out.println("✅ Préstamo creado con ID: " + loanId);

        // STEP 4: Verify loan exists in database
        assertTrue(loanRepository.findById(loanId).isPresent());
        Loan dbLoan = loanRepository.findById(loanId).get();
        assertEquals(LoanStatus.ACTIVE, dbLoan.getStatus());
        assertEquals("E2E test loan", dbLoan.getNotes());
        System.out.println("✅ Préstamo verificado en base de datos");

        // STEP 5: Get the loan via API
        mockMvc.perform(get("/api/loans/" + loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        System.out.println("✅ Préstamo obtenido via API");

        // STEP 6: Return the book via API
        System.out.println("📚 Devolviendo libro...");
        mockMvc.perform(patch("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());

        // STEP 7: Verify return in database
        Loan returnedLoan = loanRepository.findById(loanId).get();
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
        assertNotNull(returnedLoan.getReturnDate());
        System.out.println("✅ Libro devuelto correctamente");

        // STEP 8: Verify external service interactions
        verify(userServiceClient, atLeastOnce()).getUserById(1L);
        verify(userServiceClient, atLeastOnce()).validateUser(1L);
        verify(bookServiceClient, atLeastOnce()).getBookById(1L);
        verify(bookServiceClient, atLeastOnce()).isBookAvailable(1L);
        verify(bookServiceClient).updateAvailability(1L, -1); // Loan creation
        verify(bookServiceClient).updateAvailability(1L, 1);  // Book return
        System.out.println("✅ Servicios externos llamados correctamente");

        System.out.println("🎉 Test E2E completado exitosamente");
    }

    @Test
    void createQuickLoan_ShouldWork() throws Exception {
        System.out.println("🧪 Probando creación rápida de préstamo");

        // Setup mocks
        setupMocksForSuccessfulLoan();

        // Test quick loan creation
        mockMvc.perform(post("/api/loans/quick")
                        .param("userId", "1")
                        .param("bookId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify loan was created in database
        assertEquals(1, loanRepository.count());
        System.out.println("✅ Préstamo rápido creado correctamente");
    }

    @Test
    void getAllLoans_ShouldReturnActualData() throws Exception {
        System.out.println("🧪 Probando obtener todos los préstamos");

        // Create loans directly in database
        Loan loan1 = createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        Loan loan2 = createLoanInDatabase(2L, 2L, LoanStatus.RETURNED);

        // Call API
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        System.out.println("✅ Lista de préstamos obtenida correctamente");
    }

    @Test
    void loanStatistics_ShouldReflectRealData() throws Exception {
        System.out.println("🧪 Probando estadísticas de préstamos");

        // Create various loans with different statuses
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(2L, 2L, LoanStatus.RETURNED);
        createLoanInDatabase(3L, 3L, LoanStatus.OVERDUE);

        // Call statistics API
        mockMvc.perform(get("/api/loans/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans").value(3))
                .andExpect(jsonPath("$.activeLoans").value(1))
                .andExpect(jsonPath("$.returnedLoans").value(1))
                .andExpect(jsonPath("$.overdueLoans").value(1));

        System.out.println("✅ Estadísticas obtenidas correctamente");
    }

    @Test
    void userLoanLimits_ShouldBeEnforced() throws Exception {
        System.out.println("🧪 Probando límites de préstamos por usuario");

        // Setup BASIC user (limit: 3 books)
        testUser.setMembershipType("BASIC");
        setupMocksForSuccessfulLoan();

        // Create 3 loans (user limit)
        for (int i = 0; i < 3; i++) {
            LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
            request.setUserId(1L);
            request.setBookId((long) (i + 1)); // Different books

            // Mock different books
            BookDto book = createTestBook((long) (i + 1));
            when(bookServiceClient.getBookById((long) (i + 1))).thenReturn(book);
            when(bookServiceClient.isBookAvailable((long) (i + 1))).thenReturn(true);

            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify 3 loans exist
        assertEquals(3, loanRepository.count());
        System.out.println("✅ 3 préstamos creados correctamente");

        // Try to create 4th loan (should fail)
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(4L);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // UserNotValidException -> 403

        // Verify still only 3 loans
        assertEquals(3, loanRepository.count());
        System.out.println("✅ Límite de préstamos aplicado correctamente");
    }

    private void setupMocksForSuccessfulLoan() {
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        doNothing().when(bookServiceClient).updateAvailability(any(Long.class), any(Integer.class));
    }

    private BookDto createTestBook(Long id) {
        BookDto book = new BookDto();
        book.setId(id);
        book.setTitle("Book " + id);
        book.setAuthor("Author " + id);
        book.setAvailableCopies(5);
        return book;
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
