package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Comment;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.dto.CommentLikeResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoCommentResponse;
import com.tecnocampus.LS2.protube_back.repository.CommentLikeRepository;
import com.tecnocampus.LS2.protube_back.repository.CommentRepository;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Service
public class CommentService {
    private static final int MAX_REPLY_DEPTH = 8;
    private final VideoRepository videos;
    private final CommentRepository comments;
    private final UserRepository users;
    private final CommentLikeRepository likes;
    private final ObjectMapper mapper;
    private final Path store;

    public CommentService(VideoRepository videos, CommentRepository comments, UserRepository users,
                          CommentLikeRepository likes, ObjectMapper mapper,
                          @Value("${pro_tube.store.dir}") String store) {
        this.videos = videos;
        this.comments = comments;
        this.users = users;
        this.likes = likes;
        this.mapper = mapper;
        this.store = Path.of(store);
    }

    @Transactional(readOnly = true)
    public List<VideoCommentResponse> getComments(long videoId, JsonNode importedComments, Long viewerId) {
        List<Entry> entries = entries(videoId, importedComments);
        Map<String, List<Entry>> children = new HashMap<>();
        for (Entry entry : entries) {
            if (entry.parent() != null) children.computeIfAbsent(entry.parent(), key -> new ArrayList<>()).add(entry);
        }
        Map<String, Long> counts = new HashMap<>();
        Set<String> liked = new HashSet<>();
        for (var like : likes.findByVideoId(videoId)) {
            counts.merge(like.getCommentKey(), 1L, Long::sum);
            if (Objects.equals(viewerId, like.getUserId())) liked.add(like.getCommentKey());
        }
        return entries.stream().filter(entry -> entry.parent() == null)
                .map(entry -> response(entry, children, counts, liked)).toList();
    }

    @Transactional
    public VideoCommentResponse addComment(long videoId, long userId, String text) {
        return save(findVideo(videoId), userId, text, null);
    }

    @Transactional
    public VideoCommentResponse reply(long videoId, String parentId, long userId, String text) {
        Video video = findVideo(videoId);
        List<Entry> entries = entries(videoId, importedComments(video));
        Map<String, Entry> byId = new HashMap<>();
        entries.forEach(entry -> byId.put(entry.id(), entry));
        Entry parent = target(entries, parentId);
        int depth = 0;
        while (parent.parent() != null) {
            parent = byId.get(parent.parent());
            if (parent == null || ++depth >= MAX_REPLY_DEPTH) throw tooDeep();
        }
        return save(video, userId, text, parentId);
    }

    @Transactional
    public CommentLikeResponse setLiked(long videoId, String commentId, long userId, boolean liked) {
        Video video = findVideo(videoId);
        Entry entry = target(entries(videoId, importedComments(video)), commentId);
        findUser(userId);
        if (liked) likes.insertIfAbsent(videoId, commentId, userId);
        else likes.removeLike(videoId, commentId, userId);
        return new CommentLikeResponse(entry.baseline() + likes.countByVideoIdAndCommentKey(videoId, commentId),
                likes.existsByVideoIdAndCommentKeyAndUser_Id(videoId, commentId, userId));
    }

    private VideoCommentResponse save(Video video, long userId, String text, String parent) {
        String clean = text == null ? "" : text.trim();
        if (clean.isBlank() || clean.length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a comment of 1–5000 characters.");
        }
        Comment saved = comments.saveAndFlush(new Comment(video, findUser(userId), clean,
                System.currentTimeMillis() / 1000L, 0, parent));
        return new VideoCommentResponse("local-" + saved.getId(), saved.getAuthor().getEmail(), saved.getText(),
                saved.getCreatedAt(), 0, false, List.of());
    }

    private List<Entry> entries(long videoId, JsonNode imported) {
        List<Entry> result = new ArrayList<>();
        if (imported != null && imported.isArray()) {
            for (int index = 0; index < imported.size(); index++) {
                JsonNode node = imported.get(index);
                result.add(new Entry("imported-" + index, null, node.path("author").asText(""),
                        node.path("text").asText(""), node.path("timestamp").asLong(0),
                        Math.max(0, node.path("like_count").asLong(0))));
            }
        }
        for (Comment comment : comments.findByVideoIdOrderByCreatedAtAscIdAsc(videoId)) {
            result.add(new Entry("local-" + comment.getId(), comment.getParentCommentKey(),
                    comment.getAuthor().getEmail(), comment.getText(), comment.getCreatedAt(),
                    Math.max(0, comment.getLikeCount())));
        }
        // Stable sorting preserves source order and numeric database ID order within a second.
        result.sort(Comparator.comparingLong(Entry::timestamp));
        return result;
    }

    private VideoCommentResponse response(Entry entry, Map<String, List<Entry>> children,
                                          Map<String, Long> counts, Set<String> liked) {
        return new VideoCommentResponse(entry.id(), entry.author(), entry.text(), entry.timestamp(),
                entry.baseline() + counts.getOrDefault(entry.id(), 0L), liked.contains(entry.id()),
                children.getOrDefault(entry.id(), List.of()).stream()
                        .map(child -> response(child, children, counts, liked)).toList());
    }

    private JsonNode importedComments(Video video) {
        if (video.getSourceId() == null) return mapper.createArrayNode();
        try {
            JsonNode metadata = mapper.readTree(store.resolve(video.getSourceId() + ".json").toFile());
            if (metadata == null || !metadata.isObject()) throw new VideoNotFoundException(video.getId());
            return metadata.path("meta").path("comments");
        } catch (IOException exception) {
            throw new VideoNotFoundException(video.getId());
        }
    }

    private Video findVideo(long id) {
        return videos.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
    }

    private User findUser(long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static Entry target(List<Entry> entries, String id) {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found in this video."));
    }

    private static ResponseStatusException tooDeep() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please reply to an earlier comment in this discussion.");
    }

    private record Entry(String id, String parent, String author, String text, long timestamp, long baseline) {}
}
