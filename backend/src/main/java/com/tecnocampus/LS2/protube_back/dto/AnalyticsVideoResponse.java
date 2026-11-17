package com.tecnocampus.LS2.protube_back.dto;

public record AnalyticsVideoResponse(
        Long id,
        String title,
        long views,
        long comments,
        long likes) {}
