package com.innowise.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final SecurityProperties securityProperties;

    @Bean
    public SecurityWebFilterChain security(ServerHttpSecurity http) {

        String[] publicPaths = securityProperties.getPublicPaths()
                .toArray(new String[0]);

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(ex -> ex
                        .pathMatchers(publicPaths).permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
