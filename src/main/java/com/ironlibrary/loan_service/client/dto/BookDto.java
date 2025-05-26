package com.ironlibrary.loan_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para comunicación con Book Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private Integer totalCopies;
    private Integer availableCopies;

    public boolean isAvailable() {
        return availableCopies != null && availableCopies > 0;
    }
}
