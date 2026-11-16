package com.tecnocampus.LS2.protube_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VideoCommentRequest(
        @NotBlank(message = "Comment cannot be empty.")
        @Size(max = 5000, message = "Comment must contain at most 5000 characters.") String text) {
    public VideoCommentRequest {
        text = text == null ? null : text.trim();
    }
}
