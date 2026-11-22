package com.lifeos.ai.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lifeos.ai")
public record AiProperties(boolean enabled, String provider) {
    public AiProperties {
        if (provider == null || provider.isBlank()) provider = "disabled";
    }
}
