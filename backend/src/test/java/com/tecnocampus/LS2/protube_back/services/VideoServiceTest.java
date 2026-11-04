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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoServiceTest {
    @TempDir Path store;

    @Test
    void returnsSummariesAndStablePageMetadata() {
        var repository = mock(VideoRepository.class);
        var request = PageRequest.of(1, 12, Sort.by("id").descending());
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

    @Test
    void storesCommentForAuthenticatedUser() throws Exception {
        var repository = mock(VideoRepository.class);
        var userRepository = mock(com.tecnocampus.LS2.protube_back.repository.UserRepository.class);
        var commentRepository = mock(com.tecnocampus.LS2.protube_back.repository.CommentRepository.class);
        var video = new Video("Title", "1.webp", "1.mp4", 77L);
        when(repository.findById(1L)).thenReturn(Optional.of(video));
        when(userRepository.findById(10L)).thenReturn(Optional.of(new com.tecnocampus.LS2.protube_back.domain.User("viewer@example.com", "hash")));
        when(commentRepository.save(any(com.tecnocampus.LS2.protube_back.domain.Comment.class)))
                .thenAnswer(invocation -> {
                    var comment = invocation.getArgument(0, com.tecnocampus.LS2.protube_back.domain.Comment.class);
                    return new com.tecnocampus.LS2.protube_back.domain.Comment(comment.getVideo(), comment.getAuthor(), comment.getText(), comment.getCreatedAt(), comment.getLikeCount());
                });
        var result = new VideoService(repository, commentRepository, userRepository, new ObjectMapper(), store.toString())
                .addComment(1L, 10L, " Great! ");
        assertEquals("Great!", result.text());
        assertEquals("viewer@example.com", result.author());
    }

    @Test
    void ownerCanUpdateMetadataAndNonOwnerIsRejected() {
        var repository = mock(VideoRepository.class);
        var owner = mock(com.tecnocampus.LS2.protube_back.domain.User.class);
        when(owner.getId()).thenReturn(10L);
        var video = new Video("Title", "1.webp", "1.mp4", 77L, owner, "Old", "Old category", List.of("old"));
        when(repository.findById(1L)).thenReturn(Optional.of(video));
        when(repository.saveAndFlush(video)).thenReturn(video);
        var service = service(repository);
        var updated = service.updateMetadata(1L, 10L,
                new com.tecnocampus.LS2.protube_back.dto.VideoUpdateRequest(" New ", " Science ", List.of(" physics ")));
        assertEquals("New", updated.description());
        assertEquals("Science", updated.category());
        assertEquals(List.of("physics"), updated.tags());
        verify(repository).saveAndFlush(video);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.updateMetadata(1L, 11L,
                        new com.tecnocampus.LS2.protube_back.dto.VideoUpdateRequest("No", "No", new ArrayList<>())));
    }

    @Test
    void uploadedDetailsUseDatabaseWithoutSidecar() throws Exception {
        var repository = mock(VideoRepository.class);
        var video = new Video("Uploaded", "upload.webp", "upload.mp4", null, null, "Database description", "", List.of());
        when(repository.findById(42L)).thenReturn(Optional.of(video));
        Files.writeString(store.resolve("upload.mp4"), "video");
        Files.writeString(store.resolve("upload.webp"), "thumbnail");
        var result = service(repository).getVideoDetails(42L);
        assertEquals("Database description", result.description());
        assertTrue(result.comments().isEmpty());
        Files.delete(store.resolve("upload.mp4"));
        assertThrows(VideoNotFoundException.class, () -> service(repository).getVideoDetails(42L));
    }

    private VideoService service(VideoRepository repository) {
        var userRepository = mock(com.tecnocampus.LS2.protube_back.repository.UserRepository.class);
        var commentRepository = mock(com.tecnocampus.LS2.protube_back.repository.CommentRepository.class);
        return new VideoService(repository, commentRepository, userRepository, new ObjectMapper(), store.toString());
    }
}
