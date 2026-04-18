package com.youssef.ecomeraauthservice.auth.controller;

import com.youssef.ecomeraauthservice.auth.dto.request.ChangePasswordRequest;
import com.youssef.ecomeraauthservice.auth.dto.request.LoginRequest;
import com.youssef.ecomeraauthservice.auth.dto.request.RefreshRequest;
import com.youssef.ecomeraauthservice.auth.dto.request.RegisterRequest;
import com.youssef.ecomeraauthservice.auth.dto.response.AuthResponse;
import com.youssef.ecomeraauthservice.auth.dto.response.MeResponse;
import com.youssef.ecomeraauthservice.auth.service.AuthService;
import com.youssef.ecomeraauthservice.common.exception.UnauthorizedException;
import com.youssef.ecomeraauthservice.common.util.HttpUtils;
import com.youssef.ecomeraauthservice.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints for user registration, login, and token management")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register new user",
            description = "Creates a new user account with email, password, name, and role. Returns JWT access and refresh tokens upon successful registration."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registration successful - Returns JWT tokens",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input data - Validation failed",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "timestamp": "2026-01-22T10:00:00",
                                      "status": 400,
                                      "error": "Validation Failed",
                                      "message": "Invalid request parameters",
                                      "validationErrors": {
                                        "email": "Invalid email format",
                                        "password": "Password must be at least 8 characters"
                                      }
                                    }
                                    """
                    )
            )
    )

    @ApiResponse(
            responseCode = "409",
            description = "Email already registered",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "timestamp": "2026-01-22T10:00:00",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "Email already registered: user@example.com"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Parameter(
                    description = "User registration details including email, password, first name, last name, and role",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "firstName": "John",
                                              "lastName": "Doe",
                                              "email": "john.doe@example.com",
                                              "password": "SecurePass123!",
                                              "role": "USER"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password. Returns JWT access and refresh tokens. Also updates last login timestamp and IP address."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentication successful - Returns JWT tokens",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials - Wrong email or password",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "timestamp": "2026-01-22T10:00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "Invalid email or password"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Account disabled or locked"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Parameter(
                    description = "User login credentials",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "john.doe@example.com",
                                              "password": "SecurePass123!"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest,
            @Parameter(description = "Client IP address (forwarded from proxy)", example = "192.168.1.100")
            String ipAddress
    ) {
        // Provide default if header missing
        String clientIp = ipAddress != null ? ipAddress : HttpUtils.getClientIp(httpServletRequest);

        return ResponseEntity.ok(authService.authenticate(request, clientIp));
    }

    @Operation(
            summary = "Get current user",
            description = "Retrieves the currently authenticated user's profile information from JWT token",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Current user retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MeResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                      "email": "john.doe@example.com",
                                      "firstName": "John",
                                      "lastName": "Doe",
                                      "role": "USER",
                                      "lastLogin": "2026-01-22T10:00:00",
                                      "ipAddress": "192.168.1.100"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated - No valid JWT token provided"
    )
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }

        return ResponseEntity.ok(authService.whoami(user));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token. Send refresh token in Authorization header as 'Bearer {token}'",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully - Returns new access token",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )

    @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
    )

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshRequest authHeader) {

        AuthResponse response = authService.refreshToken(authHeader);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-password")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Change current user's password", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid password data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }

        authService.changePassword(req, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Logout",
            description = "Revokes current access token"
    )
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader) {

        return ResponseEntity.ok(authService.logout(authHeader));
    }
}