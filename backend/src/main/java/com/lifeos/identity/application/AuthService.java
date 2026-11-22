package com.lifeos.identity.application;

import com.lifeos.household.domain.Household;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.domain.HouseholdRole;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.household.infra.HouseholdRepository;
import com.lifeos.identity.domain.PasswordResetToken;
import com.lifeos.identity.domain.User;
import com.lifeos.identity.infra.PasswordResetTokenRepository;
import com.lifeos.identity.infra.UserRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.time.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    public AuthService(UserRepository userRepository, HouseholdRepository householdRepository,
                       HouseholdMembershipRepository membershipRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder, TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email already registered");
        }
        String locale = blankToDefault(request.locale(), "pt-BR");
        String timezone = blankToDefault(request.timezone(), "America/Sao_Paulo");
        User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim(), locale, timezone);
        userRepository.save(user);

        Instant now = timeProvider.now();
        Household household = new Household(defaultHouseholdName(user.getDisplayName()), user.getId());
        householdRepository.save(household);

        HouseholdMembership membership = new HouseholdMembership(household.getId(), user.getId(), HouseholdRole.OWNER, now);
        membershipRepository.save(membership);

        authenticate(user, httpRequest);
        return toAuthResponse(user, household, HouseholdRole.OWNER);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        authenticate(user, httpRequest);
        HouseholdMembership membership = membershipRepository.findByUserId(user.getId()).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "HOUSEHOLD_MISSING", "User has no household"));
        Household household = householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "HOUSEHOLD_MISSING", "Household not found"));
        return toAuthResponse(user, household, membership.getRole());
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
        HouseholdMembership membership = membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "HOUSEHOLD_MISSING", "User has no household"));
        Household household = householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "HOUSEHOLD_MISSING", "Household not found"));
        return toAuthResponse(user, household, membership.getRole());
    }

    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString() + UUID.randomUUID();
            String hash = sha256(token);
            Instant now = timeProvider.now();
            PasswordResetToken resetToken = new PasswordResetToken(UUID.randomUUID(), user.getId(), hash, now.plus(RESET_TOKEN_TTL), now);
            passwordResetTokenRepository.save(resetToken);
            // MVP foundation: no email provider configured. Log token only in local/dev via explicit message; never log in prod config.
            log.info("Password reset requested for userId={}. Token issued for manual delivery in dev.", user.getId());
            log.debug("DEV_PASSWORD_RESET_TOKEN userId={} token={}", user.getId(), token);
        });
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        String hash = sha256(token);
        Instant now = timeProvider.now();
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Invalid or expired reset token"));
        if (resetToken.isUsed() || resetToken.isExpired(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Invalid or expired reset token");
        }
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Invalid or expired reset token"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.markUsed(now);
        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }

    private void authenticate(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private AuthDtos.AuthResponse toAuthResponse(User user, Household household, HouseholdRole role) {
        return new AuthDtos.AuthResponse(
                new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getLocale(), user.getTimezone()),
                new AuthDtos.HouseholdSummary(household.getId(), household.getName(), role.name())
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String defaultHouseholdName(String displayName) {
        String base = displayName == null || displayName.isBlank() ? "Personal" : displayName.trim();
        return base + " Space";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
