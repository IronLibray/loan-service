package com.ironlibrary.loan_service.client;

import com.ironlibrary.loan_service.client.dto.BookDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Cliente Feign para comunicación con Book Service
 */
@FeignClient(name = "book-service")
public interface BookServiceClient {

    @GetMapping("/api/books/{id}")
    BookDto getBookById(@PathVariable("id") Long id);

    @GetMapping("/api/books/{id}/available")
    Boolean isBookAvailable(@PathVariable("id") Long id);

    @PatchMapping("/api/books/{id}/availability")
    void updateAvailability(@PathVariable("id") Long id, @RequestParam("copies") int copies);
}
