package com.tecnocampus.LS2.protube_back.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VideoUpdateRequest(
        @NotNull @Size(max = 5000, message = "Description must contain at most 5000 characters.")
        String description,
        @NotNull @Size(max = 120, message = "Category must contain at most 120 characters.")
        String category,
        @NotNull @Size(max = 20, message = "A maximum of 20 tags is allowed.")
        List<@NotNull @Size(max = 50, message = "Tags must contain at most 50 characters.") String> tags) {
    public VideoUpdateRequest {
        description = description == null ? null : description.trim();
        category = category == null ? null : category.trim();
        tags = tags == null ? null : tags.stream().map(tag -> tag == null ? null : tag.trim())
                .filter(tag -> tag != null && !tag.isBlank()).distinct().toList();
    }
}
