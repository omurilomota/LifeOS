package com.lifeos.identity.api;

import com.lifeos.identity.application.AuthDtos;
import com.lifeos.identity.application.AuthService;
import com.lifeos.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest);
    }

    @PostMapping("/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse demo(HttpServletRequest httpRequest) {
        return authService.demo(httpRequest);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public AuthDtos.AuthResponse me() {
        return authService.me(SecurityUtils.currentUserId());
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> requestPasswordReset(@Valid @RequestBody AuthDtos.PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return Map.of("status", "accepted");
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody AuthDtos.PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.token(), request.newPassword());
    }
}
