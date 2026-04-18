package com.ecomera.auth.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AuthResponse(
        @JsonProperty("access_token")
        @Schema(description = "JWT access token")
        String accessToken,
        @JsonProperty("refresh_token")
        @Schema(description = "JWT refresh token")
        String refreshToken
) {
}
