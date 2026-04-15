package com.innowise.gateway.service;

import com.innowise.gateway.controller.client.AuthClient;
import com.innowise.gateway.controller.client.UserClient;
import com.innowise.gateway.dto.AuthRequest;
import com.innowise.gateway.dto.AuthResponse;
import com.innowise.gateway.dto.CreateUserDTO;
import com.innowise.gateway.dto.RegisterRequest;
import com.innowise.gateway.exception.UserAlreadyExistsException;
import com.innowise.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AuthClient authClient;
    private final UserClient userClient;

    private final JwtUtil jwtUtil;

    @Value("${internal.key}")
    private String internalKey;

    public Mono<AuthResponse> register(RegisterRequest req) {

        return authClient.register(
            AuthRequest.builder()
                    .login(req.getLogin())
                    .password(req.getPassword())
                    .build()
            )
            .flatMap(auth -> {

            String userId = jwtUtil.extractUserId(auth.getAccessToken());

            return userClient.create(
                CreateUserDTO.builder()
                    .name(req.getName())
                    .email(req.getEmail())
                    .active(true)
                    .birthDate(req.getBirthDate())
                    .surname(req.getSurname())
                    .build())

                .thenReturn(auth)

                    .onErrorResume(ex ->
                        authClient.deleteUser(Long.valueOf(userId), internalKey)
                            .then(Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Registration failed, rollback done: " + ex.getMessage()
                                )
                            ))
                        );
            });
    }
}
