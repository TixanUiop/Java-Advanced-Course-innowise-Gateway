package com.innowise.gateway.service;

import com.innowise.gateway.client.AuthClient;
import com.innowise.gateway.client.UserClient;
import com.innowise.gateway.dto.AuthRequest;
import com.innowise.gateway.dto.AuthResponse;
import com.innowise.gateway.dto.CreateUserDTO;
import com.innowise.gateway.dto.RegisterRequest;
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

            Long userId = extractUserIdSafely(auth);

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
                        authClient.deleteUser(userId, internalKey)
                            .then(Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Registration failed, rollback done: " + ex.getMessage()
                                )
                            ))
                        );
            });
    }

    private Long extractUserIdSafely(AuthResponse auth) {
        try {
            return Long.valueOf(jwtUtil.extractUserId(auth.getAccessToken()));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid token, cannot extract userId"
            );
        }
    }
}
