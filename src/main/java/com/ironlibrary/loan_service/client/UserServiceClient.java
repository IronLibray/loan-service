package com.ironlibrary.loan_service.client;

import com.ironlibrary.loan_service.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Cliente Feign para comunicación con User Service
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/users/{id}/validate")
    Boolean validateUser(@PathVariable("id") Long id);
}
