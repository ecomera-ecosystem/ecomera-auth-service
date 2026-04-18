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
import com.ecomera.auth.user.User;
import com.ecomera.auth.user.UserMapper;
import com.ecomera.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email());
        }

        // Build user with SuperBuilder (because User extends BaseEntity)
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generate tokens
        String jwtToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Save access token
        saveUserToken(savedUser, jwtToken, TokenType.ACCESS);
        saveUserToken(savedUser, refreshToken, TokenType.REFRESH);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request, String clientIp) {
        var userPasswordAuthToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(userPasswordAuthToken);

        // Find user
        User user = (User) authentication.getPrincipal();

        // Generate tokens
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Revoke old tokens and save new one
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken, TokenType.ACCESS);
        saveUserToken(user, refreshToken, TokenType.REFRESH);

        // Update last login
        recordLogin(user, clientIp);
        userRepository.save(user);

        log.info("User authenticated successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();

        // Validate refresh token
        Token storedToken = tokenRepository.findByValue(refreshToken)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            throw new BusinessException("Refresh token invalid");
        }

        User user = storedToken.getUser();

        String accessToken = jwtService.generateToken(user);

        revokeAllUserTokens(user);

        saveUserToken(user, accessToken, TokenType.ACCESS);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    private void recordLogin(User user, String ip) {
        user.setLastLogin(LocalDateTime.now());
        user.setIpAddress(ip);
    }

    public MeResponse whoami(User user) {
        return userMapper.toMeResponse(user);
    }

    public void changePassword(ChangePasswordRequest req, User user) {
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new IllegalStateException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public Map<String, String> logout(String authHeader){

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("No token provided");
        }

        String jwt = authHeader.substring(7);

        tokenRepository.findByValue(jwt).ifPresent(token -> {
            token.setExpired(true);
            token.setRevoked(true);
            tokenRepository.save(token);
        });

        SecurityContextHolder.clearContext();

        return Map.of("message", "Logged out successfully");
    }

    // ========================================
    // HELPERS
    // ========================================

    private void saveUserToken(User user, String jwtToken, TokenType tokenType) {
        Token token = Token.builder()
                .user(user)
                .value(jwtToken)
                .tokenType(tokenType)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());

        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }
}