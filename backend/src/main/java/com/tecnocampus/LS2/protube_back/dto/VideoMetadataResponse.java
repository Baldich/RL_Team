package com.tecnocampus.LS2.protube_back.dto;

import java.util.List;

public record VideoMetadataResponse(Long id, String description, String category, List<String> tags, Long ownerId) {}
