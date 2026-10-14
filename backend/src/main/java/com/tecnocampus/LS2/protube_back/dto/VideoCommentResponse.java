package com.tecnocampus.LS2.protube_back.dto;

public record VideoCommentResponse(String author, String text, long timestamp, int likeCount) {}
