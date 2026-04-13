package com.youssef.ecomeraauthservice.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RefreshRequest(
        @Schema(description = "JWT refresh token")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}