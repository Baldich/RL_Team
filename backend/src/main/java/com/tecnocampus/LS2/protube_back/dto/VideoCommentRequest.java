package com.tecnocampus.LS2.protube_back.dto;

import jakarta.validation.constraints.NotBlank;

public record VideoCommentRequest(@NotBlank(message = "Comment cannot be empty.") String text) {}
