package com.innowise.gateway.unit;

import com.innowise.gateway.client.AuthClient;
import com.innowise.gateway.client.UserClient;
import com.innowise.gateway.dto.AuthRequest;
import com.innowise.gateway.dto.AuthResponse;
import com.innowise.gateway.dto.CreateUserDTO;
import com.innowise.gateway.dto.RegisterRequest;
import com.innowise.gateway.exception.UserAlreadyExistsException;
import com.innowise.gateway.service.RegistrationService;
import com.innowise.gateway.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    AuthClient authClient;

    @Mock
    UserClient userClient;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    RegistrationService registrationService;

    RegisterRequest req;

    @BeforeEach
    void setUp() {
        req = RegisterRequest.builder()
                .name("test")
                .login("test")
                .birthDate(LocalDate.of(2003, 12, 30))
                .surname("test")
                .email("test@test.com")
                .password("test")
                .build();

        ReflectionTestUtils.setField(registrationService, "internalKey", "test-internal-key");


    }

    @Test
    void registerShouldPropagateRollbackErrorWhenDeleteUserAlsoFails() {
        AuthRequest authRequest = AuthRequest.builder()
                .login(req.getLogin())
                .password(req.getPassword())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .build();

        CreateUserDTO createDto = CreateUserDTO.builder()
                .name(req.getName())
                .email(req.getEmail())
                .active(true)
                .birthDate(req.getBirthDate())
                .surname(req.getSurname())
                .build();

        when(authClient.register(authRequest)).thenReturn(Mono.just(authResponse));
        when(jwtUtil.extractUserId(authResponse.getAccessToken())).thenReturn("123");
        when(userClient.create(createDto)).thenReturn(Mono.error(new RuntimeException("Create failed")));
        when(authClient.deleteUser(eq(123L), anyString())).thenReturn(Mono.error(new RuntimeException("Delete also failed")));

        Mono<AuthResponse> register = registrationService.register(req);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> register.block());

        Assertions.assertEquals("Delete also failed", exception.getMessage());

        verify(authClient, times(1)).register(authRequest);
        verify(jwtUtil, times(1)).extractUserId(authResponse.getAccessToken());
        verify(userClient, times(1)).create(createDto);
        verify(authClient, times(1)).deleteUser(eq(123L), anyString());
    }

    @Test
    void registerShouldRollbackWhenUserClientCreateFails() {
        AuthRequest authRequest = AuthRequest.builder()
                .login(req.getLogin())
                .password(req.getPassword())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .build();

        CreateUserDTO createDto = CreateUserDTO.builder()
                .name(req.getName())
                .email(req.getEmail())
                .active(true)
                .birthDate(req.getBirthDate())
                .surname(req.getSurname())
                .build();

        when(authClient.register(authRequest)).thenReturn(Mono.just(authResponse));
        when(jwtUtil.extractUserId(authResponse.getAccessToken())).thenReturn("123");
        when(userClient.create(createDto)).thenReturn(Mono.error(new RuntimeException("Create failed")));
        when(authClient.deleteUser(eq(123L), anyString())).thenReturn(Mono.empty());

        Mono<AuthResponse> register = registrationService.register(req);

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> register.block());

        Assertions.assertEquals(400, exception.getStatusCode().value());
        Assertions.assertTrue(exception.getMessage().contains("Registration failed, rollback done"));

        verify(authClient, times(1)).register(authRequest);
        verify(jwtUtil, times(1)).extractUserId(authResponse.getAccessToken());
        verify(userClient, times(1)).create(createDto);
        verify(authClient, times(1)).deleteUser(eq(123L), anyString());
    }

    @Test
    void registerShouldThrowResponseStatusExceptionWhenExtractUserIdFails() {

        AuthRequest authRequest = AuthRequest.builder()
                .login(req.getLogin())
                .password(req.getPassword())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("invalid.token")
                .refreshToken("refresh_token")
                .build();

        when(authClient.register(authRequest)).thenReturn(Mono.just(authResponse));
        when(jwtUtil.extractUserId(authResponse.getAccessToken()))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Invalid token, cannot extract userId"));


        Mono<AuthResponse> register = registrationService.register(req);

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> register.block());

        Assertions.assertEquals(500, exception.getStatusCode().value());
        Assertions.assertEquals("Invalid token, cannot extract userId", exception.getReason());

        verify(authClient, times(1)).register(authRequest);
        verify(jwtUtil, times(1)).extractUserId(authResponse.getAccessToken());
        verify(userClient, never()).create(any());
        verify(authClient, never()).deleteUser(any(), anyString());
    }

    @Test
    void registerShouldThrowUserAlreadyExistsExceptionWithLoginExists() {

        AuthRequest authRequest = AuthRequest.builder()
                .login(req.getLogin())
                .password(req.getPassword())
                .build();

        String expectedMessage = "User with login already exist: " + req.getLogin();

        when(authClient.register(authRequest)).thenReturn(
                Mono.error(new UserAlreadyExistsException("User with login already exist: " +  authRequest.getLogin())));


        Mono<AuthResponse> register = registrationService.register(req);
        UserAlreadyExistsException userAlreadyExistsException = Assertions.assertThrows(UserAlreadyExistsException.class, () -> register.block());

        Assertions.assertEquals(expectedMessage, userAlreadyExistsException.getMessage());
        verify(authClient, times(1)).register(authRequest);
    }

    @Test
    void registerShouldReturnAuthResponseWhenSuccess() {

        AuthRequest authRequest = AuthRequest.builder()
                .login(req.getLogin())
                .password(req.getPassword())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access_token_success")
                .refreshToken("refresh_token_success")
                .build();

        CreateUserDTO createDto = CreateUserDTO.builder()
                .name(req.getName())
                .email(req.getEmail())
                .active(true)
                .birthDate(req.getBirthDate())
                .surname(req.getSurname())
                .build();

        when(authClient.register(authRequest)).thenReturn(Mono.just(authResponse));

        when(jwtUtil.extractUserId(authResponse.getAccessToken())).thenReturn("1");

        when(userClient.create(createDto)).thenReturn(Mono.empty());


        Mono<AuthResponse> register = registrationService.register(req);
        AuthResponse actualResult = register.block();

        Assertions.assertNotNull(actualResult);
        Assertions.assertEquals(authResponse.getAccessToken(), actualResult.getAccessToken());
        Assertions.assertEquals(authResponse.getRefreshToken(), actualResult.getRefreshToken());

        verify(authClient, times(1)).register(authRequest);
        verify(jwtUtil, times(1)).extractUserId(authResponse.getAccessToken());
        verify(userClient, times(1)).create(createDto);
        verify(authClient, never()).deleteUser(any(), anyString());

    }

}
