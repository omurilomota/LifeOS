package com.lifeos.identity.application;

import java.util.UUID;

public record CurrentUser(UUID id, String email, String displayName, String locale, String timezone) {
}
