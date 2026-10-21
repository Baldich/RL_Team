package com.tecnocampus.LS2.protube_back.dto;

import java.util.List;

public record VideoPageResponse(List<VideoSummaryResponse> content, int page, int size,
                                long totalElements, int totalPages) {}
