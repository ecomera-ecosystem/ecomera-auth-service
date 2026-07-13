package com.ecomera.auth.auth.service;

import com.ecomera.auth.auth.dto.request.ChangePasswordRequest;
import com.ecomera.auth.auth.dto.request.LoginRequest;
import com.ecomera.auth.auth.dto.request.RefreshRequest;
import com.ecomera.auth.auth.dto.request.RegisterRequest;
import com.ecomera.auth.auth.dto.response.AuthResponse;
import com.ecomera.auth.auth.dto.response.MeResponse;
import com.ecomera.auth.common.exception.BusinessException;
import com.ecomera.auth.common.exception.UnauthorizedException;
import com.ecomera.auth.security.JwtService;
import com.ecomera.auth.token.Token;
import com.ecomera.auth.token.TokenRepository;
import com.ecomera.auth.token.TokenType;
import com.ecomera.auth.user.Role;
import com.ecomera.auth.user.User;
import com.ecomera.auth.user.UserMapper;
import com.ecomera.auth.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<Token> tokenCaptor;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String password = "password123";
    private final String encodedPassword = "$2a$12$encoded";
    private final String accessToken = "access.jwt.token";
    private final String refreshToken = "refresh.jwt.token";

    private User createUser() {
        return User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .password(encodedPassword)
                .role(Role.USER)
                .lastLogin(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .build();
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .password(password)
                .role(Role.USER)
                .build();
        User user = createUser();

        given(userRepository.existsByEmail(email)).willReturn(false);
        given(passwordEncoder.encode(password)).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtService.generateToken(anyMap(), any())).willReturn(accessToken);
        given(jwtService.generateRefreshToken(any())).willReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);

        then(userRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(email);
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(encodedPassword);

        then(tokenRepository).should(times(2)).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getAllValues()).hasSize(2);
        assertThat(tokenCaptor.getAllValues().get(0).getTokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(tokenCaptor.getAllValues().get(1).getTokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void register_shouldThrowBusinessException_whenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .password(password)
                .role(Role.USER)
                .build();

        given(userRepository.existsByEmail(email)).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");

        then(userRepository).should(never()).save(any());
    }

    @Test
    void authenticate_shouldReturnTokens_whenValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        User user = createUser();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);
        given(jwtService.generateToken(anyMap(), any())).willReturn(accessToken);
        given(jwtService.generateRefreshToken(any())).willReturn(refreshToken);
        given(tokenRepository.findAllValidTokenByUser(userId)).willReturn(List.of());

        AuthResponse response = authService.authenticate(request, "192.168.1.1");

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);

        then(userRepository).should().save(any(User.class));
        then(tokenRepository).should(times(2)).save(any(Token.class));
    }

    @Test
    void authenticate_shouldThrowBadCredentialsException_whenInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("wrongpassword")
                .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(request, "192.168.1.1"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshToken_shouldReturnNewAccessToken_whenValidRefreshToken() {
        User user = createUser();
        Token storedToken = Token.builder()
                .value(refreshToken)
                .tokenType(TokenType.REFRESH)
                .revoked(false)
                .expired(false)
                .user(user)
                .build();
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        given(tokenRepository.findByValue(refreshToken)).willReturn(Optional.of(storedToken));
        given(jwtService.generateToken(anyMap(), any())).willReturn(accessToken);
        given(tokenRepository.findAllValidTokenByUser(userId)).willReturn(List.of());

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);

        then(tokenRepository).should().save(any(Token.class));
    }

    @Test
    void refreshToken_shouldThrowBusinessException_whenRefreshTokenNotFound() {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("invalid-token")
                .build();

        given(tokenRepository.findByValue("invalid-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_shouldThrowBusinessException_whenRefreshTokenRevoked() {
        User user = createUser();
        Token storedToken = Token.builder()
                .value(refreshToken)
                .tokenType(TokenType.REFRESH)
                .revoked(true)
                .expired(false)
                .user(user)
                .build();
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        given(tokenRepository.findByValue(refreshToken)).willReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh token invalid");
    }

    @Test
    void refreshToken_shouldThrowBusinessException_whenRefreshTokenExpired() {
        User user = createUser();
        Token storedToken = Token.builder()
                .value(refreshToken)
                .tokenType(TokenType.REFRESH)
                .revoked(false)
                .expired(true)
                .user(user)
                .build();
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        given(tokenRepository.findByValue(refreshToken)).willReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh token invalid");
    }

    @Test
    void whoami_shouldReturnMeResponse() {
        User user = createUser();
        MeResponse expected = MeResponse.builder()
                .id(userId.toString())
                .email(email)
                .firstName("John")
                .lastName("Doe")
                .role("USER")
                .build();

        given(userMapper.toMeResponse(user)).willReturn(expected);

        MeResponse response = authService.whoami(user);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void changePassword_shouldUpdatePassword_whenOldPasswordMatches() {
        User user = createUser();
        String newPassword = "newPassword123";
        String newEncoded = "$2a$12$newEncoded";
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword(password)
                .newPassword(newPassword)
                .confirmPassword(newPassword)
                .build();

        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn(newEncoded);

        authService.changePassword(request, user);

        assertThat(user.getPassword()).isEqualTo(newEncoded);
        then(userRepository).should().save(user);
    }

    @Test
    void changePassword_shouldThrowBadCredentialsException_whenOldPasswordMismatch() {
        User user = createUser();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongOldPassword")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        given(passwordEncoder.matches("wrongOldPassword", encodedPassword)).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request, user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Wrong password");
    }

    @Test
    void changePassword_shouldThrowIllegalStateException_whenNewPasswordsDoNotMatch() {
        User user = createUser();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword(password)
                .newPassword("newPassword123")
                .confirmPassword("differentPassword")
                .build();

        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);

        assertThatThrownBy(() -> authService.changePassword(request, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    void logout_shouldRevokeTokenAndClearContext() {
        String authHeader = "Bearer " + accessToken;
        Token token = Token.builder()
                .value(accessToken)
                .revoked(false)
                .expired(false)
                .build();

        given(tokenRepository.findByValue(accessToken)).willReturn(Optional.of(token));

        Map<String, String> result = authService.logout(authHeader);

        assertThat(result).containsEntry("message", "Logged out successfully");
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isExpired()).isTrue();
        then(tokenRepository).should().save(token);
    }

    @Test
    void logout_shouldThrowUnauthorizedException_whenAuthHeaderIsNull() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No token provided");
    }

    @Test
    void logout_shouldThrowUnauthorizedException_whenAuthHeaderIsNotBearer() {
        assertThatThrownBy(() -> authService.logout("Basic someToken"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No token provided");
    }

    @Test
    void logout_shouldSucceed_whenTokenNotFoundInDb() {
        String authHeader = "Bearer " + accessToken;

        given(tokenRepository.findByValue(accessToken)).willReturn(Optional.empty());

        Map<String, String> result = authService.logout(authHeader);

        assertThat(result).containsEntry("message", "Logged out successfully");
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void authenticate_shouldRevokeOldTokens_whenTheyExist() {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        User user = createUser();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        Token oldToken = Token.builder()
                .value("old-token")
                .revoked(false)
                .expired(false)
                .user(user)
                .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);
        given(jwtService.generateToken(anyMap(), any())).willReturn(accessToken);
        given(jwtService.generateRefreshToken(any())).willReturn(refreshToken);
        given(tokenRepository.findAllValidTokenByUser(userId)).willReturn(List.of(oldToken));

        authService.authenticate(request, "192.168.1.1");

        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.isExpired()).isTrue();
        then(tokenRepository).should().saveAll(anyList());
    }
}
