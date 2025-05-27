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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test End-to-End MODERNO para Spring Boot 3.4+
 * Sin @MockBean deprecated, usando @TestConfiguration con @Primary
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class LibrarySystemE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserServiceClient userServiceClient; // Mock via TestConfiguration

    @Autowired
    private BookServiceClient bookServiceClient; // Mock via TestConfiguration

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUser;
    private BookDto testBook;

    /**
     * Configuración moderna que reemplaza @MockBean
     */
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
        // IMPORTANTE: Reset de mocks antes de cada test
        reset(userServiceClient, bookServiceClient);

        // Limpiar base de datos
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
    void completeLibraryWorkflow_ShouldWorkEndToEnd() throws Exception {
        // SCENARIO: Complete library workflow
        // 1. Create a loan
        // 2. Verify loan exists
        // 3. Return the book
        // 4. Verify book was returned

        // STEP 1: Create a loan via API
        setupMocksForSuccessfulLoan();

        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("E2E test loan");

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

        // Parse response to get loan ID
        Loan createdLoan = objectMapper.readValue(response, Loan.class);
        Long loanId = createdLoan.getId();

        // STEP 2: Verify loan exists in database
        assertTrue(loanRepository.findById(loanId).isPresent());
        Loan dbLoan = loanRepository.findById(loanId).get();
        assertEquals(LoanStatus.ACTIVE, dbLoan.getStatus());
        assertEquals("E2E test loan", dbLoan.getNotes());

        // STEP 3: Verify we can get the loan via API
        mockMvc.perform(get("/api/loans/" + loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // STEP 4: Return the book via API
        mockMvc.perform(patch("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());

        // STEP 5: Verify return in database
        Loan returnedLoan = loanRepository.findById(loanId).get();
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
        assertNotNull(returnedLoan.getReturnDate());

        // STEP 6: Verify external service interactions
        verify(userServiceClient).getUserById(1L);
        verify(userServiceClient).validateUser(1L);
        verify(bookServiceClient).getBookById(1L);
        verify(bookServiceClient).isBookAvailable(1L);
        verify(bookServiceClient).updateAvailability(1L, -1); // Loan creation
        verify(bookServiceClient).updateAvailability(1L, 1);  // Book return
    }

    @Test
    void userLoanLimits_ShouldBeEnforcedEndToEnd() throws Exception {
        // SCENARIO: Test user limits enforcement
        // 1. Create loans up to user limit (BASIC = 3)
        // 2. Try to create one more loan (should fail)

        // Setup BASIC user (limit: 3 books)
        testUser.setMembershipType("BASIC");
        setupMocksForSuccessfulLoan();

        // STEP 1: Create 3 loans (user limit)
        for (int i = 0; i < 3; i++) {
            LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
            request.setUserId(1L);
            request.setBookId((long) (i + 1)); // Different books
            request.setNotes("Loan " + (i + 1));

            // Mock different books
            BookDto book = new BookDto();
            book.setId((long) (i + 1));
            book.setTitle("Book " + (i + 1));
            book.setAvailableCopies(5);
            when(bookServiceClient.getBookById((long) (i + 1))).thenReturn(book);
            when(bookServiceClient.isBookAvailable((long) (i + 1))).thenReturn(true);

            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // STEP 2: Verify 3 loans exist in database
        assertEquals(3, loanRepository.count());
        assertEquals(3, loanRepository.findByUserId(1L).size());

        // STEP 3: Try to create 4th loan (should fail)
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(4L);
        request.setNotes("Should fail");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // UserNotValidException -> 403

        // STEP 4: Verify still only 3 loans
        assertEquals(3, loanRepository.count());
    }

    @Test
    void duplicateBookLoan_ShouldBePreventedEndToEnd() throws Exception {
        // SCENARIO: Prevent user from borrowing same book twice

        setupMocksForSuccessfulLoan();

        // STEP 1: Create first loan
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("First loan");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // STEP 2: Try to create duplicate loan (same user, same book)
        LoanController.CreateLoanRequest duplicateRequest = new LoanController.CreateLoanRequest();
        duplicateRequest.setUserId(1L);
        duplicateRequest.setBookId(1L); // Same book
        duplicateRequest.setNotes("Duplicate loan");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest()); // IllegalArgumentException -> 400

        // STEP 3: Verify only one loan exists
        assertEquals(1, loanRepository.count());
    }

    @Test
    void loanExtension_ShouldWorkEndToEnd() throws Exception {
        // SCENARIO: Test loan extension functionality

        setupMocksForSuccessfulLoan();

        // STEP 1: Create loan
        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Extension test");

        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Loan createdLoan = objectMapper.readValue(response, Loan.class);
        Long loanId = createdLoan.getId();
        LocalDate originalDueDate = createdLoan.getDueDate();

        // STEP 2: Extend loan by 7 days
        mockMvc.perform(patch("/api/loans/" + loanId + "/extend")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId));

        // STEP 3: Verify extension in database
        Loan extendedLoan = loanRepository.findById(loanId).get();
        assertEquals(originalDueDate.plusDays(7), extendedLoan.getDueDate());
    }

    @Test
    void overdueLoans_ShouldBeDetectedEndToEnd() throws Exception {
        // SCENARIO: Test overdue loan detection

        // STEP 1: Create loan with past due date directly in database
        Loan overdueLoan = new Loan();
        overdueLoan.setUserId(1L);
        overdueLoan.setBookId(1L);
        overdueLoan.setLoanDate(LocalDate.now().minusDays(20));
        overdueLoan.setDueDate(LocalDate.now().minusDays(5)); // 5 days overdue
        overdueLoan.setStatus(LoanStatus.ACTIVE);
        overdueLoan.setNotes("Overdue test");

        Loan savedLoan = loanRepository.save(overdueLoan);

        // STEP 2: Call overdue API
        mockMvc.perform(get("/api/loans/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(savedLoan.getId()))
                .andExpect(jsonPath("$[0].status").value("OVERDUE")); // Should be updated to OVERDUE

        // STEP 3: Verify status was updated in database
        Loan updatedLoan = loanRepository.findById(savedLoan.getId()).get();
        assertEquals(LoanStatus.OVERDUE, updatedLoan.getStatus());
    }

    @Test
    void loanStatistics_ShouldReflectRealDataEndToEnd() throws Exception {
        // SCENARIO: Test statistics with real data

        // STEP 1: Create various loans with different statuses
        // Active loan
        Loan activeLoan = new Loan();
        activeLoan.setUserId(1L);
        activeLoan.setBookId(1L);
        activeLoan.setLoanDate(LocalDate.now());
        activeLoan.setDueDate(LocalDate.now().plusDays(14));
        activeLoan.setStatus(LoanStatus.ACTIVE);
        loanRepository.save(activeLoan);

        // Returned loan
        Loan returnedLoan = new Loan();
        returnedLoan.setUserId(2L);
        returnedLoan.setBookId(2L);
        returnedLoan.setLoanDate(LocalDate.now().minusDays(10));
        returnedLoan.setDueDate(LocalDate.now().minusDays(3));
        returnedLoan.setReturnDate(LocalDate.now().minusDays(1));
        returnedLoan.setStatus(LoanStatus.RETURNED);
        loanRepository.save(returnedLoan);

        // Overdue loan
        Loan overdueLoan = new Loan();
        overdueLoan.setUserId(3L);
        overdueLoan.setBookId(3L);
        overdueLoan.setLoanDate(LocalDate.now().minusDays(30));
        overdueLoan.setDueDate(LocalDate.now().minusDays(5));
        overdueLoan.setStatus(LoanStatus.OVERDUE);
        loanRepository.save(overdueLoan);

        // STEP 2: Call statistics API
        mockMvc.perform(get("/api/loans/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans").value(3))
                .andExpect(jsonPath("$.activeLoans").value(1))
                .andExpect(jsonPath("$.returnedLoans").value(1))
                .andExpect(jsonPath("$.overdueLoans").value(1));
    }

    @Test
    void serviceFailure_ShouldBeHandledGracefullyEndToEnd() throws Exception {
        // SCENARIO: Test error handling when external services fail

        // STEP 1: Mock user service failure
        when(userServiceClient.getUserById(1L))
                .thenThrow(new RuntimeException("User service unavailable"));

        LoanController.CreateLoanRequest request = new LoanController.CreateLoanRequest();
        request.setUserId(1L);
        request.setBookId(1L);
        request.setNotes("Should fail");

        // STEP 2: Attempt to create loan - should return 403 (UserNotValidException)
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // UserNotValidException mapped to 403

        // STEP 3: Verify no loan was created
        assertEquals(0, loanRepository.count());
    }

    @Test
    void getAllLoans_ShouldReturnAllLoansFromDatabase() throws Exception {
        // SCENARIO: Test that API returns actual data from database

        // STEP 1: Create loans directly in database
        Loan loan1 = createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        Loan loan2 = createLoanInDatabase(2L, 2L, LoanStatus.RETURNED);

        // STEP 2: Call API
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(loan1.getId()))
                .andExpect(jsonPath("$[1].id").value(loan2.getId()));
    }

    @Test
    void getUserLoans_ShouldReturnUserSpecificLoans() throws Exception {
        // SCENARIO: Test filtering by user

        // Setup user service mock
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        // STEP 1: Create loans for different users
        createLoanInDatabase(1L, 1L, LoanStatus.ACTIVE);
        createLoanInDatabase(1L, 2L, LoanStatus.ACTIVE);
        createLoanInDatabase(2L, 3L, LoanStatus.ACTIVE); // Different user

        // STEP 2: Get loans for user 1
        mockMvc.perform(get("/api/loans/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[1].userId").value(1));

        verify(userServiceClient).getUserById(1L);
    }

    private void setupMocksForSuccessfulLoan() {
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        doNothing().when(bookServiceClient).updateAvailability(any(Long.class), any(Integer.class));
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
