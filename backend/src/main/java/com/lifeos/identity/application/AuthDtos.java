package com.lifeos.identity.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 120) String displayName,
            String locale,
            String timezone
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record PasswordResetRequest(
            @Email @NotBlank String email
    ) {}

    public record PasswordResetConfirmRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {}

    public record UserResponse(
            UUID id,
            String email,
            String displayName,
            String locale,
            String timezone
    ) {}

    public record AuthResponse(
            UserResponse user,
            HouseholdSummary currentHousehold
    ) {}

    public record HouseholdSummary(
            UUID id,
            String name,
            String role
    ) {}
}
