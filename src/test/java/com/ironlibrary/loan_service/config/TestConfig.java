package com.ironlibrary.loan_service.config;

import com.ironlibrary.loan_service.client.BookServiceClient;
import com.ironlibrary.loan_service.client.UserServiceClient;
import com.ironlibrary.loan_service.client.dto.BookDto;
import com.ironlibrary.loan_service.client.dto.UserDto;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Configuración unificada para todos los tests
 * Excluye Feign y Eureka para evitar conflictos
 */
@TestConfiguration
@Profile("test")
@EnableAutoConfiguration(exclude = {
        FeignAutoConfiguration.class,
        EurekaClientAutoConfiguration.class,

})
public class TestConfig {

    @Bean
    @Primary
    public UserServiceClient mockUserServiceClient() {
        UserServiceClient mock = mock(UserServiceClient.class);

        // Setup default behavior
        UserDto defaultUser = createDefaultUser();
        when(mock.getUserById(anyLong())).thenReturn(defaultUser);
        when(mock.validateUser(anyLong())).thenReturn(true);

        return mock;
    }

    @Bean
    @Primary
    public BookServiceClient mockBookServiceClient() {
        BookServiceClient mock = mock(BookServiceClient.class);

        // Setup default behavior
        BookDto defaultBook = createDefaultBook();
        when(mock.getBookById(anyLong())).thenReturn(defaultBook);
        when(mock.isBookAvailable(anyLong())).thenReturn(true);
        doNothing().when(mock).updateAvailability(anyLong(), any(Integer.class));

        return mock;
    }

    private UserDto createDefaultUser() {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setMembershipType("PREMIUM");
        user.setIsActive(true);
        user.setRegistrationDate(LocalDate.now());
        return user;
    }

    private BookDto createDefaultBook() {
        BookDto book = new BookDto();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("978-123-456-789");
        book.setCategory("FICTION");
        book.setTotalCopies(10);
        book.setAvailableCopies(5);
        return book;
    }
}
