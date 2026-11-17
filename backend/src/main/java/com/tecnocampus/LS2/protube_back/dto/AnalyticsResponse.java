package com.tecnocampus.LS2.protube_back.dto;

import java.util.List;

public record AnalyticsResponse(
        List<AnalyticsVideoResponse> videos,
        long totalViews,
        long totalComments,
        long totalLikes) {}
