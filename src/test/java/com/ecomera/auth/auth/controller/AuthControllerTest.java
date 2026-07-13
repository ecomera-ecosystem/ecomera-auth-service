package com.ecomera.auth.auth.controller;

import com.ecomera.auth.auth.dto.request.ChangePasswordRequest;
import com.ecomera.auth.auth.dto.request.LoginRequest;
import com.ecomera.auth.auth.dto.request.RefreshRequest;
import com.ecomera.auth.auth.dto.request.RegisterRequest;
import com.ecomera.auth.auth.dto.response.AuthResponse;
import com.ecomera.auth.auth.dto.response.MeResponse;
import com.ecomera.auth.auth.service.AuthService;
import com.ecomera.auth.security.JwtAuthEntryPoint;
import com.ecomera.auth.security.JwtAuthFilter;
import com.ecomera.auth.user.Role;
import com.ecomera.auth.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String accessToken = "access.jwt.token";
    private final String refreshToken = "refresh.jwt.token";

    private User createUser() {
        return User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .password("$2a$12$encoded")
                .role(Role.USER)
                .lastLogin(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .build();
    }

    private UsernamePasswordAuthenticationToken createAuth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
    }

    @Test
    void register_shouldReturn200() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .password("password123")
                .role(Role.USER)
                .build();
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        given(authService.register(any(RegisterRequest.class))).willReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(accessToken))
                .andExpect(jsonPath("$.refresh_token").value(refreshToken));
    }

    @Test
    void login_shouldReturn200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("password123")
                .build();
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        given(authService.authenticate(any(LoginRequest.class), anyString())).willReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(accessToken))
                .andExpect(jsonPath("$.refresh_token").value(refreshToken));
    }

    @Test
    void me_shouldReturn200() throws Exception {
        User user = createUser();
        MeResponse meResponse = MeResponse.builder()
                .id(userId.toString())
                .email(email)
                .firstName("John")
                .lastName("Doe")
                .role("USER")
                .build();

        given(authService.whoami(user)).willReturn(meResponse);

        SecurityContextHolder.getContext().setAuthentication(createAuth(user));
        try {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value(email));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void me_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_shouldReturn200() throws Exception {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken(refreshToken)
                .build();

        given(authService.refreshToken(any(RefreshRequest.class))).willReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.refresh_token").value(refreshToken));
    }

    @Test
    void changePassword_shouldReturn204() throws Exception {
        User user = createUser();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass123")
                .newPassword("newPass123")
                .confirmPassword("newPass123")
                .build();

        SecurityContextHolder.getContext().setAuthentication(createAuth(user));
        try {
            mockMvc.perform(patch("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void changePassword_shouldReturn401_whenNotAuthenticated() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass123")
                .newPassword("newPass123")
                .confirmPassword("newPass123")
                .build();

        mockMvc.perform(patch("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        User user = createUser();
        Map<String, String> response = Map.of("message", "Logged out successfully");

        given(authService.logout("Bearer " + accessToken)).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .with(authentication(createAuth(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void register_shouldReturn400_whenInvalidBody() throws Exception {
        String invalidBody = """
                {
                    "firstName": "",
                    "email": "not-an-email",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn400_whenInvalidBody() throws Exception {
        String invalidBody = """
                {
                    "email": "",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
