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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.cloud.loadbalancer.enabled=false"
})
@Transactional
class WorkingE2ETest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

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
        System.out.println("🔧 Configurando Working E2E Test");

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Reset mocks
        reset(userServiceClient, bookServiceClient);

        // Clear database
        loanRepository.deleteAll();

        // Setup test data
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@email.com");
        testUser.setMembershipType("PREMIUM");
        testUser.setIsActive(true);
        testUser.setRegistrationDate(LocalDate.now());

        testBook = new BookDto();
        testBook.setId(1L);
        testBook.setTitle("Cien años de soledad");
        testBook.setAuthor("Gabriel García Márquez");
        testBook.setIsbn("978-123-456-789");
        testBook.setCategory("FICTION");
        testBook.setTotalCopies(10);
        testBook.setAvailableCopies(5);

        System.out.println("✅ Setup completado - MockMvc configurado manualmente");
    }

    @Test
    void healthCheck_ShouldWork() throws Exception {
        System.out.println("🧪 Working E2E Test: Health Check");

        mockMvc.perform(get("/api/loans/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Loan Service is running on port 8083"));

        System.out.println("✅ Health check OK");
    }

    @Test
    void createQuickLoan_ShouldWork() throws Exception {
        System.out.println("🧪 Working E2E Test: Create Quick Loan");

        setupMocksForSuccess();

        mockMvc.perform(post("/api/loans/quick")
                        .param("userId", "1")
                        .param("bookId", "1"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify in database
        assertEquals(1L, loanRepository.count());
        System.out.println("✅ Quick loan creado y verificado en BD");
    }

    @Test
    void createLoanWithBody_ShouldWork() throws Exception {
        System.out.println("🧪 Working E2E Test: Create Loan with Body");

        setupMocksForSuccess();

        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Working E2E test loan");

        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.notes").value("Working E2E test loan"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse response and verify
        Loan createdLoan = objectMapper.readValue(response, Loan.class);
        assertNotNull(createdLoan.getId());

        // Verify in database
        assertTrue(loanRepository.findById(createdLoan.getId()).isPresent());
        System.out.println("✅ Préstamo creado con ID: " + createdLoan.getId());
    }

    @Test
    void completeWorkflow_ShouldWork() throws Exception {
        System.out.println("🧪 Working E2E Test: Complete Workflow");

        setupMocksForSuccess();

        // STEP 1: Create loan
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Complete workflow test");

        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Loan createdLoan = objectMapper.readValue(response, Loan.class);
        Long loanId = createdLoan.getId();
        System.out.println("✅ Paso 1: Préstamo creado con ID " + loanId);

        // STEP 2: Get the loan
        mockMvc.perform(get("/api/loans/" + loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        System.out.println("✅ Paso 2: Préstamo obtenido via API");

        // STEP 3: Get all loans
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
        System.out.println("✅ Paso 3: Lista de préstamos obtenida");

        // STEP 4: Extend loan
        mockMvc.perform(patch("/api/loans/" + loanId + "/extend")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId));
        System.out.println("✅ Paso 4: Préstamo extendido");

        // STEP 5: Return book
        mockMvc.perform(patch("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());
        System.out.println("✅ Paso 5: Libro devuelto");

        // STEP 6: Verify in database
        Loan finalLoan = loanRepository.findById(loanId).orElse(null);
        assertNotNull(finalLoan);
        assertEquals(LoanStatus.RETURNED, finalLoan.getStatus());
        assertNotNull(finalLoan.getReturnDate());
        System.out.println("✅ Paso 6: Estado verificado en BD");

        // STEP 7: Get statistics
        mockMvc.perform(get("/api/loans/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans").value(1))
                .andExpect(jsonPath("$.returnedLoans").value(1));
        System.out.println("✅ Paso 7: Estadísticas verificadas");

        System.out.println("🎉 ¡Workflow completo E2E exitoso!");
    }

    @Test
    void userLimits_ShouldBeEnforced() throws Exception {
        System.out.println("🧪 Working E2E Test: User Limits");

        // Setup BASIC user (limit: 3)
        testUser.setMembershipType("BASIC");
        setupMocksForSuccess();

        // Create 3 loans
        for (int i = 1; i <= 3; i++) {
            BookDto book = createTestBook((long) i);
            when(bookServiceClient.getBookById((long) i)).thenReturn(book);
            when(bookServiceClient.isBookAvailable((long) i)).thenReturn(true);

            LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
            request.setUserId(1L);
            request.setBookId((long) i);

            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        assertEquals(3L, loanRepository.count());
        System.out.println("✅ 3 préstamos creados (límite alcanzado)");

        // Try 4th loan (should fail)
        BookDto book4 = createTestBook(4L);
        when(bookServiceClient.getBookById(4L)).thenReturn(book4);
        when(bookServiceClient.isBookAvailable(4L)).thenReturn(true);

        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(4L);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("límite")));

        assertEquals(3L, loanRepository.count());
        System.out.println("✅ 4to préstamo rechazado correctamente");
    }

    private void setupMocksForSuccess() {
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        doNothing().when(bookServiceClient).updateAvailability(anyLong(), any(Integer.class));
    }

    private BookDto createTestBook(Long id) {
        BookDto book = new BookDto();
        book.setId(id);
        book.setTitle("Test Book " + id);
        book.setAuthor("Test Author " + id);
        book.setIsbn("978-123-456-" + String.format("%03d", id));
        book.setCategory("FICTION");
        book.setTotalCopies(10);
        book.setAvailableCopies(5);
        return book;
    }
}
