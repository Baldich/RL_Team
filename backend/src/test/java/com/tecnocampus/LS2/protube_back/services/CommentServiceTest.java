package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Comment;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentServiceTest {
    @TempDir Path store;
    final VideoRepository videos = mock(VideoRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final CommentRepository comments = mock(CommentRepository.class);
    final CommentLikeRepository likes = mock(CommentLikeRepository.class);
    final ObjectMapper mapper = new ObjectMapper();
    Video video;
    User user;
    CommentService service;

    @BeforeEach void setup() {
        video = new Video("Uploaded", "v.webp", "v.mp4", null);
        user = new User("viewer@example.com", "hash");
        when(videos.findById(1L)).thenReturn(Optional.of(video));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        service = new CommentService(videos, comments, users, likes, mapper, store.toString());
    }

    Comment row(long id, String parent) throws Exception {
        var comment = new Comment(video, user, "Comment " + id, id, 0, parent);
        var field = Comment.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(comment, id);
        return comment;
    }

    @Test void createsTrimmedRootAndNestedRepliesWithStableReferences() throws Exception {
        when(comments.saveAndFlush(any())).thenAnswer(call -> {
            Comment comment = call.getArgument(0);
            var id = Comment.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(comment, 9L);
            return comment;
        });
        var root = service.addComment(1, 7, " Root ");
        assertEquals("local-9", root.id());
        assertEquals("Root", root.text());
        when(comments.findByVideoIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(row(9, null)));
        var reply = service.reply(1, "local-9", 7, " Reply ");
        assertEquals("Reply", reply.text());
        verify(comments).saveAndFlush(argThat(comment -> "local-9".equals(comment.getParentCommentKey())));
    }

    @Test void returnsSortedNestedThreadsAndPreservesHistoricalCounts() throws Exception {
        when(comments.findByVideoIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(
                row(10, "imported-0"), row(11, "local-10"), row(12, null)));
        var imported = mapper.readTree("[{\"author\":\"source\",\"text\":\"Original\",\"timestamp\":1,\"like_count\":4}]");
        var result = service.getComments(1, imported, null);
        assertEquals(2, result.size());
        assertEquals("imported-0", result.getFirst().id());
        assertEquals(4, result.getFirst().likeCount());
        assertFalse(result.getFirst().likedByCurrentUser());
        assertEquals("local-11", result.getFirst().replies().getFirst().replies().getFirst().id());
        assertEquals("local-12", result.getLast().id());
        assertTrue(service.getComments(2, null, null).isEmpty());
    }

    @Test void rejectsMissingVideoUserTargetsAndInvalidTextWithoutWrites() {
        assertThrows(VideoNotFoundException.class, () -> service.reply(99, "local-1", 7, "Reply"));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.setLiked(1, "local-99", 7, true)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> service.reply(1, "../bad", 7, "Reply"));
        for (String text : new String[]{null, " ", "x".repeat(5001)}) {
            assertThrows(ResponseStatusException.class, () -> service.addComment(1, 7, text));
        }
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> service.addComment(1, 99, "Reply")).getStatusCode().value());
        verify(comments, never()).saveAndFlush(any());
        verifyNoInteractions(likes);
    }

    @Test void commentsPostedInTheSameSecondKeepNumericCreationOrder() throws Exception {
        Comment first = row(2, null);
        Comment next = row(10, null);
        var timestamp = Comment.class.getDeclaredField("createdAt");
        timestamp.setAccessible(true);
        timestamp.setLong(first, 123);
        timestamp.setLong(next, 123);
        when(comments.findByVideoIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(first, next));
        assertEquals(List.of("local-2", "local-10"), service.getComments(1, null, null).stream()
                .map(com.tecnocampus.LS2.protube_back.dto.VideoCommentResponse::id).toList());
    }

    @Test void boundsThreadDepthBeforeSavingAnotherReply() throws Exception {
        List<Comment> chain = new ArrayList<>();
        for (int i = 0; i <= 8; i++) chain.add(row(i + 1, i == 0 ? null : "local-" + i));
        when(comments.findByVideoIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(chain);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.reply(1, "local-9", 7, "Too deep")).getStatusCode().value());
        verify(comments, never()).saveAndFlush(any());
    }

    @Test void unavailableOrInvalidSourceMetadataCannotReceiveReactions() throws Exception {
        Video imported = mock(Video.class);
        when(imported.getId()).thenReturn(1L);
        when(imported.getSourceId()).thenReturn(77L);
        when(videos.findById(1L)).thenReturn(Optional.of(imported));
        assertThrows(VideoNotFoundException.class, () -> service.setLiked(1, "imported-0", 7, true));
        Files.writeString(store.resolve("77.json"), "null");
        assertThrows(VideoNotFoundException.class, () -> service.reply(1, "imported-0", 7, "Reply"));
        Files.writeString(store.resolve("77.json"), "broken");
        assertThrows(VideoNotFoundException.class, () -> service.setLiked(1, "imported-0", 7, false));
        verifyNoInteractions(likes);
    }
}
