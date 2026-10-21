package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoServiceTest {
    @TempDir Path store;

    @Test
    void returnsSummariesAndStablePageMetadata() {
        var repository = mock(VideoRepository.class);
        var request = PageRequest.of(1, 12, Sort.by("id").ascending());
        when(repository.findAll(request)).thenReturn(new PageImpl<>(List.of(new Video("Title", "0.webp", "0.mp4", 0L)), request, 13));
        var result = service(repository).getVideos(1, 12);
        assertEquals("Title", result.content().get(0).title());
        assertEquals("0.webp", result.content().get(0).thumbnailPath());
        assertEquals(1, result.page());
        assertEquals(12, result.size());
        assertEquals(13, result.totalElements());
        assertEquals(2, result.totalPages());
        verify(repository).findAll(request);
    }

    @Test
    void emptyCatalogIsSuccessful() {
        var repository = mock(VideoRepository.class);
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 12)));
        var result = service(repository).getVideos(0, 12);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalPages());
    }

    @Test
    void returnsVideoDetailsWithDescriptionAndComments() throws Exception {
        var repository = mock(VideoRepository.class);
        var video = new Video("Title", "1.webp", "1.mp4", 77L);
        when(repository.findById(1L)).thenReturn(Optional.of(video));
        Files.writeString(store.resolve("77.json"), """
                {
                  "meta": {
                    "description": "A sample description",
                    "comments": [
                      {"author": "@viewer", "text": "Nice!", "timestamp": 123, "like_count": 4}
                    ]
                  }
                }
                """);
        Files.writeString(store.resolve("1.mp4"), "video");
        Files.writeString(store.resolve("1.webp"), "thumb");
        var result = service(repository).getVideoDetails(1L);
        assertEquals("Title", result.title());
        assertEquals("A sample description", result.description());
        assertEquals("1.mp4", result.videoPath());
        assertEquals("1.webp", result.thumbnailPath());
        assertEquals(1, result.comments().size());
        assertEquals("@viewer", result.comments().get(0).author());
        assertEquals("Nice!", result.comments().get(0).text());
    }

    @Test
    void missingVideoOrFilesReturnsNotFound() {
        var repository = mock(VideoRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(VideoNotFoundException.class, () -> service(repository).getVideoDetails(1L));
    }

    private VideoService service(VideoRepository repository) {
        return new VideoService(repository, new ObjectMapper(), store.toString());
    }
}
