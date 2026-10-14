package com.tecnocampus.LS2.protube_back.dto;

import java.util.List;

public record VideoDetailResponse(Long id, String title, String description, String videoPath,
                                  String thumbnailPath, List<VideoCommentResponse> comments) {}
