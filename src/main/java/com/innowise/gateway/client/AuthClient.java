package com.innowise.gateway.client;

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

    @Value("${clients.auth.url-register}")
    private String registerUrl;

    @Value("${clients.auth.url-delete}")
    private String deleteUrl;


    public Mono<AuthResponse> register(AuthRequest request) {
        return webClient.post()
                .uri(registerUrl)
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
                .uri(deleteUrl, id)
                .header("X-Internal-Key", key)
                .retrieve()
                .bodyToMono(Void.class);
    }
}