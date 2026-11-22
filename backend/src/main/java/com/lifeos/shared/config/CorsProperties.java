package com.lifeos.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lifeos.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
