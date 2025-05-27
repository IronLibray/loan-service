package com.ironlibrary.loan_service.test;

import com.ironlibrary.loan_service.client.dto.BookDto;
import com.ironlibrary.loan_service.client.dto.UserDto;
import com.ironlibrary.loan_service.model.Loan;
import com.ironlibrary.loan_service.model.LoanStatus;

import java.time.LocalDate;

/**
 * Clase base con utilidades comunes para tests
 */
public abstract class TestBase {

    protected UserDto createTestUser(Long id, String membershipType) {
        UserDto user = new UserDto();
        user.setId(id);
        user.setName("Test User " + id);
        user.setEmail("user" + id + "@test.com");
        user.setMembershipType(membershipType);
        user.setIsActive(true);
        user.setRegistrationDate(LocalDate.now());
        return user;
    }

    protected BookDto createTestBook(Long id) {
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

    protected Loan createTestLoan(Long userId, Long bookId, LoanStatus status) {
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

        return loan;
    }

    protected Loan createOverdueLoan(Long userId, Long bookId) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setLoanDate(LocalDate.now().minusDays(30));
        loan.setDueDate(LocalDate.now().minusDays(5)); // 5 days overdue
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setNotes("Overdue test loan");
        return loan;
    }
}
