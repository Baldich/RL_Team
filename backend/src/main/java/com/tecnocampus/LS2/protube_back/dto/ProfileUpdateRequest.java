package com.tecnocampus.LS2.protube_back.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotNull @Size(max = 80, message = "Display name must contain at most 80 characters.") String displayName,
        @NotNull @Size(max = 2048, message = "Avatar URL must contain at most 2048 characters.")
        @Pattern(regexp = "(?:https?://[^\\s]+)?", message = "Enter an HTTP or HTTPS avatar URL, or leave it empty.") String avatarUrl,
        @NotNull @Size(max = 1000, message = "Description must contain at most 1000 characters.") String description) {
    public ProfileUpdateRequest {
        displayName = displayName == null ? null : displayName.trim();
        avatarUrl = avatarUrl == null ? null : avatarUrl.trim();
        description = description == null ? null : description.trim();
    }
}
