package com.innowise.gateway;

import com.innowise.gateway.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
//SpringBootAuto SpringBootApplication
@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
