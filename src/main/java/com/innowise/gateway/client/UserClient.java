package com.innowise.gateway.client;

import com.innowise.gateway.dto.CreateUserDTO;
import com.innowise.gateway.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient webClient;

    @Value("${internal.key}")
    private String internalKey;

    @Value("${clients.user.url-create}")
    private String createUserUrl;

    public Mono<Void> create(CreateUserDTO dto) {
        return webClient.post()
                .uri(createUserUrl)
                .header("X-Internal-Key", internalKey)
                .bodyValue(dto)
                .retrieve()
                .onStatus(
                        status -> status.value() == 409,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new UserAlreadyExistsException(body)
                                ))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("User service error: " + body)
                                ))
                )
                .bodyToMono(Void.class);
    }
}