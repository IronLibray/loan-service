package com.ironlibrary.loan_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para comunicación con User Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String membershipType;
    private Boolean isActive;
    private LocalDate registrationDate;
    private String phone;
    private String address;

    public boolean canBorrowBooks() {
        return isActive != null && isActive && membershipType != null;
    }

    public int getMaxBooksAllowed() {
        if (membershipType == null) return 0;

        return switch (membershipType) {
            case "BASIC" -> 3;
            case "PREMIUM" -> 10;
            case "STUDENT" -> 5;
            default -> 0;
        };
    }

    public int getLoanDurationDays() {
        if (membershipType == null) return 14;

        return switch (membershipType) {
            case "BASIC" -> 14;
            case "PREMIUM" -> 30;
            case "STUDENT" -> 21;
            default -> 14;
        };
    }
}
