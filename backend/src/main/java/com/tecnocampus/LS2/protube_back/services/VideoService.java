package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.dto.VideoCommentResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoDetailResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoPageResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {
    private final VideoRepository videos;
    private final ObjectMapper mapper;
    private final Path storeDir;

    public VideoService(VideoRepository videos, ObjectMapper mapper,
                        @Value("${pro_tube.store.dir}") String storeDir) {
        this.videos = videos;
        this.mapper = mapper;
        this.storeDir = Paths.get(storeDir);
    }

    @Transactional(readOnly = true)
    public VideoPageResponse getVideos(int page, int size) {
        var result = videos.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
        var content = result.getContent().stream()
                .map(video -> new VideoSummaryResponse(video.getId(), video.getTitle(), video.getThumbnailPath()))
                .toList();
        return new VideoPageResponse(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoDetails(long id) {
        Video video = videos.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
        Path metadataFile = storeDir.resolve(video.getSourceId() + ".json");
        Path videoFile = storeDir.resolve(video.getVideoPath());
        Path thumbnailFile = storeDir.resolve(video.getThumbnailPath());
        if (!isReadableFile(metadataFile) || !isReadableFile(videoFile) || !isReadableFile(thumbnailFile)) {
            throw new VideoNotFoundException(id);
        }
        try {
            JsonNode metadata = mapper.readTree(metadataFile.toFile());
            String description = metadata.path("meta").path("description").asText("");
            return new VideoDetailResponse(
                    video.getId(),
                    video.getTitle(),
                    description,
                    video.getVideoPath(),
                    video.getThumbnailPath(),
                    readComments(metadata.path("meta").path("comments")));
        } catch (IOException exception) {
            throw new VideoNotFoundException(id);
        }
    }

    private static boolean isReadableFile(Path path) {
        return Files.isRegularFile(path) && Files.isReadable(path);
    }

    private static List<VideoCommentResponse> readComments(JsonNode commentsNode) {
        if (commentsNode == null || !commentsNode.isArray()) {
            return List.of();
        }
        List<VideoCommentResponse> comments = new ArrayList<>();
        for (JsonNode comment : commentsNode) {
            comments.add(new VideoCommentResponse(
                    comment.path("author").asText(""),
                    comment.path("text").asText(""),
                    comment.path("timestamp").asLong(0L),
                    comment.path("like_count").asInt(0)));
        }
        return List.copyOf(comments);
    }
}
