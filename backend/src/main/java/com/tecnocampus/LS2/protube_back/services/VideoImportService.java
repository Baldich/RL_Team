package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

@Service
public class VideoImportService {
    private static final Logger LOG = LoggerFactory.getLogger(VideoImportService.class);
    private final VideoRepository videos;
    private final ObjectMapper mapper;
    public VideoImportService(VideoRepository videos, ObjectMapper mapper) {
        this.videos = videos;
        this.mapper = mapper;
    }

    // Repository writes have separate transactions: one invalid file cannot roll back the batch.
    public void importVideos(Path store) {
        if (!Files.isDirectory(store)) {
            LOG.warn("Video store unavailable; initial catalog import skipped.");
            return;
        }
        try (var files = Files.list(store)) {
            files.filter(path -> path.getFileName().toString().matches("[0-9]+\\.json"))
                    .sorted().forEach(this::importFile);
        } catch (IOException exception) {
            LOG.warn("Unable to read video store; initial catalog import skipped.");
        }
    }

    private void importFile(Path file) {
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return;
            var metadata = mapper.readTree(file.toFile());
            if (metadata == null) {
                LOG.warn("Empty video metadata: {}", file.getFileName());
                return;
            }
            var id = metadata.path("id");
            var title = metadata.path("title");
            if (!id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() < 0
                    || !title.isTextual() || title.asText().isBlank() || title.asText().indexOf(0) >= 0) {
                LOG.warn("Invalid video metadata: {}", file.getFileName());
                return;
            }
            long sourceId = id.longValue();
            String stem = Long.toString(sourceId);
            if (!file.getFileName().toString().equals(stem + ".json")
                    || !Files.isRegularFile(file.resolveSibling(stem + ".webp"), LinkOption.NOFOLLOW_LINKS)
                    || !Files.isRegularFile(file.resolveSibling(stem + ".mp4"), LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file.resolveSibling(stem + ".webp")) == 0
                    || Files.size(file.resolveSibling(stem + ".mp4")) == 0) {
                LOG.warn("Incomplete video files: {}", file.getFileName());
                return;
            }
            if (!videos.existsBySourceId(sourceId)) {
                try {
                    videos.saveAndFlush(new Video(title.asText().trim(), stem + ".webp", stem + ".mp4", sourceId));
                } catch (DataIntegrityViolationException exception) {
                    // The database unique constraint also protects concurrent initial imports.
                    if (!videos.existsBySourceId(sourceId)) throw exception;
                }
            }
        } catch (IOException exception) {
            LOG.warn("Unreadable video metadata: {}", file.getFileName());
        }
    }
}
