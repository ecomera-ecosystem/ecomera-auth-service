package com.ecomera.auth.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponse(
        @Schema(description = "User ID") String id,
        @Schema(description = "User email") String email,
        @Schema(description = "First name") String firstName,
        @Schema(description = "Last name") String lastName,
        @Schema(description = "Role") String role,
        @Schema(description = "Last login timestamp") LocalDateTime lastLogin,
        @Schema(description = "Last known IP address") String ipAddress,
        @Schema(description = "Creation time") LocalDateTime createdAt,
        @Schema(description = "Updating time") LocalDateTime updatedAt) {
}
