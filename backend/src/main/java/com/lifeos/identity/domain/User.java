package com.lifeos.identity.domain;

import com.lifeos.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, length = 20)
    private String locale = "pt-BR";

    @Column(nullable = false, length = 64)
    private String timezone = "America/Sao_Paulo";

    protected User() {
    }

    public User(String email, String passwordHash, String displayName, String locale, String timezone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.locale = locale;
        this.timezone = timezone;
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
