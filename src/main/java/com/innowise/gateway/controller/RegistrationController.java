package com.innowise.gateway.controller;

import com.innowise.gateway.dto.AuthResponse;
import com.innowise.gateway.dto.RegisterRequest;
import com.innowise.gateway.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService service;

    @PostMapping("/auth")
    public Mono<AuthResponse> register(@RequestBody RegisterRequest req) {
        return service.register(req);
    }
}
