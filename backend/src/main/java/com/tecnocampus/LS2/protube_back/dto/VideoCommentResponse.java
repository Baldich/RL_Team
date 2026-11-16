package com.tecnocampus.LS2.protube_back.dto;

import java.util.List;

public record VideoCommentResponse(String id, String author, String text, long timestamp, long likeCount,
                                   boolean likedByCurrentUser, List<VideoCommentResponse> replies) {
    public VideoCommentResponse(String author, String text, long timestamp, int likeCount) {
        this(null, author, text, timestamp, likeCount, false, List.of());
    }
}
