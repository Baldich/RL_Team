package com.tecnocampus.LS2.protube_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VideoUploadRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 5000) String description) {
    public VideoUploadRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
    }
}
