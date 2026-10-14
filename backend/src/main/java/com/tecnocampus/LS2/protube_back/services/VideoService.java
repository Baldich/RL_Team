package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.dto.VideoPageResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoService {
    private final VideoRepository videos;
    public VideoService(VideoRepository videos) { this.videos = videos; }

    @Transactional(readOnly = true)
    public VideoPageResponse getVideos(int page, int size) {
        var result = videos.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
        var content = result.getContent().stream()
                .map(video -> new VideoSummaryResponse(video.getId(), video.getTitle(), video.getThumbnailPath()))
                .toList();
        return new VideoPageResponse(content, page, size, result.getTotalElements(), result.getTotalPages());
    }
}
