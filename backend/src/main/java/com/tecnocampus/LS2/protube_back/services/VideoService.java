package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.dto.VideoCommentResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoDetailResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoMetadataResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoPageResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoUpdateRequest;
import com.tecnocampus.LS2.protube_back.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoService {
    private final VideoRepository videos;
    private final CommentService comments;
    private final ObjectMapper mapper;
    private final Path storeDir;

    public VideoService(VideoRepository videos, CommentService comments,
                        ObjectMapper mapper, @Value("${pro_tube.store.dir}") String storeDir) {
        this.videos = videos;
        this.comments = comments;
        this.mapper = mapper;
        this.storeDir = Paths.get(storeDir);
    }

    @Transactional(readOnly = true)
    public VideoPageResponse getVideos(int page, int size) {
        return getVideos("", page, size);
    }

    @Transactional(readOnly = true)
    public VideoPageResponse getVideos(String query, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim();
        var result = normalizedQuery.isBlank()
                ? videos.findAll(PageRequest.of(page, size, Sort.by("id").descending()))
                : videos.search(normalizedQuery, PageRequest.of(page, size));
        var content = result.getContent().stream()
                .map(video -> new VideoSummaryResponse(video.getId(), video.getTitle(), video.getThumbnailPath()))
                .toList();
        return new VideoPageResponse(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoDetails(long id) {
        return getVideoDetails(id, null);
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoDetails(long id, Long viewerId) {
        Video video = videos.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
        Path metadataFile = storeDir.resolve(video.getSourceId() + ".json");
        Path videoFile = storeDir.resolve(video.getVideoPath());
        Path thumbnailFile = storeDir.resolve(video.getThumbnailPath());
        if ((video.getSourceId() != null && !isReadableFile(metadataFile)) || !isReadableFile(videoFile) || !isReadableFile(thumbnailFile)) {
            throw new VideoNotFoundException(id);
        }
        try {
            JsonNode metadata = video.getSourceId() == null ? mapper.createObjectNode() : mapper.readTree(metadataFile.toFile());
            if (metadata == null) throw new VideoNotFoundException(id);
            String description = video.hasPersistedDescription()
                    ? video.getDescription()
                    : metadata.path("meta").path("description").asText("");
            var detailComments = comments.getComments(id, metadata.path("meta").path("comments"), viewerId);
            return new VideoDetailResponse(
                    video.getId(),
                    video.getTitle(),
                    description,
                    video.getVideoPath(),
                    video.getThumbnailPath(),
                    detailComments,
                    video.getTags(),
                    video.getCategory(),
                    video.getOwnerId());
        } catch (IOException exception) {
            throw new VideoNotFoundException(id);
        }
    }

    @Transactional
    public VideoMetadataResponse updateMetadata(long videoId, long userId, VideoUpdateRequest request) {
        Video video = videos.findById(videoId).orElseThrow(() -> new VideoNotFoundException(videoId));
        if (!userOwns(video, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the video owner can edit its metadata.");
        }
        video.updateMetadata(request.description(), request.category(), request.tags());
        Video saved = videos.saveAndFlush(video);
        return new VideoMetadataResponse(saved.getId(), saved.getDescription(), saved.getCategory(),
                saved.getTags(), saved.getOwnerId());
    }

    private static boolean userOwns(Video video, long userId) {
        return video.getOwnerId() != null && video.getOwnerId() == userId;
    }

    @Transactional
    public VideoCommentResponse addComment(long videoId, long userId, String text) {
        return comments.addComment(videoId, userId, text);
    }

    private static boolean isReadableFile(Path path) {
        return Files.isRegularFile(path) && Files.isReadable(path);
    }

}
