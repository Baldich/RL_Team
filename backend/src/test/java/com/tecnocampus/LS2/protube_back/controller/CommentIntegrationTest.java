package com.tecnocampus.LS2.protube_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.PostgresTestSupport;
import com.tecnocampus.LS2.protube_back.domain.Comment;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.*;
import com.tecnocampus.LS2.protube_back.services.CommentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"pro_tube.load_initial_data=false"})
@AutoConfigureMockMvc
class CommentIntegrationTest extends PostgresTestSupport {
    static final Path STORE;
    static {
        try { STORE = Files.createTempDirectory("protube-us10-"); }
        catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("pro_tube.store.dir", () -> STORE.toString() + "/");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired VideoRepository videos;
    @Autowired UserRepository users;
    @Autowired CommentRepository comments;
    @Autowired CommentLikeRepository likes;
    @Autowired CommentService discussions;
    Video video;
    MockHttpSession first;
    MockHttpSession second;

    @BeforeEach void setup() throws Exception {
        Files.writeString(STORE.resolve("77.json"), """
                {"meta":{"comments":[{"author":"source","text":"Imported comment","timestamp":1,"like_count":4}]}}
                """);
        Files.writeString(STORE.resolve("77.mp4"), "video");
        Files.writeString(STORE.resolve("77.webp"), "thumbnail");
        video = videos.saveAndFlush(new Video("Imported", "77.webp", "77.mp4", 77L));
        first = register("first@example.com");
        second = register("second@example.com");
    }

    MockHttpSession register(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("email", email, "password", "password123"))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @AfterEach void cleanup() throws Exception {
        likes.deleteAll();
        comments.deleteAll();
        videos.deleteAll();
        users.deleteAll();
        try (var files = Files.list(STORE)) {
            for (Path file : files.toList()) Files.delete(file);
        }
    }

    String base() { return "/api/videos/" + video.getId(); }

    @Test void importedRepliesAndIndependentUserLikesPersistWithoutChangingSourceJson() throws Exception {
        String original = Files.readString(STORE.resolve("77.json"));
        String path = base() + "/comments/imported-0";
        var reply = mvc.perform(post(path + "/replies").session(first).with(csrf()).contentType("application/json")
                .content("{\"text\":\" First reply \",\"userId\":999}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("author").value("first@example.com")).andReturn();
        String replyId = mapper.readTree(reply.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(post(base() + "/comments/" + replyId + "/replies").session(second).with(csrf())
                .contentType("application/json").content("{\"text\":\"Nested reply\"}"))
                .andExpect(status().isCreated());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put(path + "/like").session(first).with(csrf())).andExpect(status().isOk())
                    .andExpect(jsonPath("likeCount").value(5)).andExpect(jsonPath("likedByCurrentUser").value(true));
        }
        mvc.perform(put(path + "/like").session(second).with(csrf()))
                .andExpect(jsonPath("likeCount").value(6));
        mvc.perform(get(base()).session(first)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("comments[0].likedByCurrentUser").value(true))
                .andExpect(jsonPath("comments[0].replies[0].text").value("First reply"))
                .andExpect(jsonPath("comments[0].replies[0].replies[0].text").value("Nested reply"));
        mvc.perform(get(base())).andExpect(status().isOk())
                .andExpect(jsonPath("comments[0].likedByCurrentUser").value(false))
                .andExpect(jsonPath("comments[0].likeCount").value(6));
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete(path + "/like").session(first).with(csrf()))
                    .andExpect(status().isOk()).andExpect(jsonPath("likeCount").value(5))
                    .andExpect(jsonPath("likedByCurrentUser").value(false));
        }
        mvc.perform(get(base()).session(second)).andExpect(jsonPath("comments[0].likedByCurrentUser").value(true));
        assertEquals(1, likes.count());
        assertEquals(2, comments.count());
        assertEquals("imported-0", comments.findByVideoIdOrderByCreatedAtAscIdAsc(video.getId()).getFirst().getParentCommentKey());
        assertEquals(original, Files.readString(STORE.resolve("77.json")));
    }

    @Test void legacyLocalCommentsAndRepliesCanBeLikedAndTargetsStayVideoScoped() throws Exception {
        var author = users.findByEmail("first@example.com").orElseThrow();
        var legacy = comments.saveAndFlush(new Comment(video, author, "Legacy root", 2, 2));
        String key = "local-" + legacy.getId();
        mvc.perform(put(base() + "/comments/" + key + "/like").session(first).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("likeCount").value(3));
        mvc.perform(delete(base() + "/comments/" + key + "/like").session(first).with(csrf()))
                .andExpect(jsonPath("likeCount").value(2));
        var reply = mvc.perform(post(base() + "/comments/" + key + "/replies").session(first).with(csrf())
                .contentType("application/json").content("{\"text\":\"Local reply\"}"))
                .andExpect(status().isCreated()).andReturn();
        String replyKey = mapper.readTree(reply.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(put(base() + "/comments/" + replyKey + "/like").session(second).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("likeCount").value(1));
        mvc.perform(get(base()).session(second))
                .andExpect(jsonPath("comments[1].id").value(key))
                .andExpect(jsonPath("comments[1].replies[0].likedByCurrentUser").value(true));

        var other = videos.saveAndFlush(new Video("Other", "v.webp", "v.mp4", null));
        String wrongTarget = "/api/videos/" + other.getId() + "/comments/" + key;
        mvc.perform(put(wrongTarget + "/like").session(first).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(wrongTarget + "/replies").session(first).with(csrf()).contentType("application/json")
                .content("{\"text\":\"Wrong video\"}")).andExpect(status().isNotFound());
        mvc.perform(delete(base() + "/comments/imported-99/like").session(first).with(csrf()))
                .andExpect(status().isNotFound());
        assertEquals(2, comments.count());
        assertEquals(1, likes.count());
    }

    @Test void concurrentRepeatedLikesCreateOnlyOnePersistentLike() throws Exception {
        long userId = users.findByEmail("first@example.com").orElseThrow().getId();
        try (var executor = Executors.newFixedThreadPool(4)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> requests = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                requests.add(executor.submit(() -> {
                    try { assertTrue(start.await(10, TimeUnit.SECONDS)); }
                    catch (InterruptedException exception) { throw new IllegalStateException(exception); }
                    discussions.setLiked(video.getId(), "imported-0", userId, true);
                }));
            }
            start.countDown();
            for (Future<?> request : requests) request.get(30, TimeUnit.SECONDS);
        }
        assertEquals(1, likes.countByVideoIdAndCommentKey(video.getId(), "imported-0"));
        mvc.perform(get(base()).session(first)).andExpect(status().isOk())
                .andExpect(jsonPath("comments[0].likeCount").value(5))
                .andExpect(jsonPath("comments[0].likedByCurrentUser").value(true));
    }
}
