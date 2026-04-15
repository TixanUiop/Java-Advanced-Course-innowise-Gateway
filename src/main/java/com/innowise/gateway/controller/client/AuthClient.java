package com.innowise.gateway.controller.client;

import com.innowise.gateway.dto.AuthRequest;
import com.innowise.gateway.dto.AuthResponse;
import com.innowise.gateway.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthClient {

    private final WebClient webClient;

    @Value("${internal.key}")
    private String internalKey;

    public Mono<AuthResponse> register(AuthRequest request) {
        return webClient.post()
                .uri("http://localhost:8081/auth/register")
                .header("X-Internal-Key", internalKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.value() == 409,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new UserAlreadyExistsException("User with login already exist: " +  request.getLogin())
                                ))
                )
                .bodyToMono(AuthResponse.class);
    }

    public Mono<Void> deleteUser(Long id, String key) {
        return webClient.delete()
                .uri("http://localhost:8081/auth/internal/{id}", id)
                .header("X-Internal-Key", key)
                .retrieve()
                .bodyToMono(Void.class);
    }
}