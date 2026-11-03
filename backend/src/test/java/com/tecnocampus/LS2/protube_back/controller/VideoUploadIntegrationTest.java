package com.tecnocampus.LS2.protube_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.PostgresTestSupport;
import com.tecnocampus.LS2.protube_back.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"pro_tube.load_initial_data=false", "ENV_PROTUBE_GOOGLE_CLIENT_ID=test", "ENV_PROTUBE_GOOGLE_CLIENT_SECRET=test"})
@AutoConfigureMockMvc
class VideoUploadIntegrationTest extends PostgresTestSupport {
    static final Path STORE;
    static { try { STORE = Files.createTempDirectory("protube-us06-test-"); } catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("pro_tube.store.dir", () -> STORE.toString() + "/");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired VideoRepository videos;
    @Autowired UserRepository users;
    @Autowired CommentRepository comments;

    @AfterEach void cleanup() throws Exception {
        comments.deleteAll();
        videos.deleteAll();
        users.deleteAll();
        try (var files = Files.list(STORE)) { for (var file : files.toList()) Files.delete(file); }
    }

    @Test void uploadPersistsAndSupportsPublicCatalogPlaybackEditingAndComments() throws Exception {
        Path fixture = Files.createTempFile("protube-fixture-", ".mp4");
        byte[] bytes;
        try {
            var process = new ProcessBuilder("ffmpeg", "-nostdin", "-y", "-v", "error", "-f", "lavfi", "-i", "color=c=blue:s=64x64:d=1",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", fixture.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try { assertTrue(process.waitFor(30, TimeUnit.SECONDS)); assertEquals(0, process.exitValue()); }
            finally { process.destroyForcibly(); }
            bytes = Files.readAllBytes(fixture);
        } finally { Files.deleteIfExists(fixture); }
        var registration = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"upload-owner@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated()).andReturn();
        var session = (MockHttpSession) registration.getRequest().getSession(false);
        long owner = mapper.readTree(registration.getResponse().getContentAsString()).path("id").asLong();
        var uploaded = mvc.perform(multipart("/api/videos")
                .file(new MockMultipartFile("file", "clip.mp4", "video/mp4", bytes))
                .file(new MockMultipartFile("metadata", "", "application/json", "{\"title\":\"Uploaded\",\"description\":\"Description\"}".getBytes()))
                .session(session).with(csrf())).andExpect(status().isCreated()).andReturn();
        var detail = mapper.readTree(uploaded.getResponse().getContentAsString());
        long id = detail.path("id").asLong();
        assertEquals(owner, videos.findById(id).orElseThrow().getOwnerId());
        assertNull(videos.findById(id).orElseThrow().getSourceId());
        mvc.perform(get("/api/videos")).andExpect(jsonPath("content[0].title").value("Uploaded"));
        mvc.perform(get("/api/videos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("description").value("Description"));
        mvc.perform(get("/media/" + detail.path("thumbnailPath").asText())).andExpect(status().isOk());
        mvc.perform(get("/media/" + detail.path("videoPath").asText()).header("Range", "bytes=0-15"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 0-15/" + bytes.length));
        mvc.perform(put("/api/videos/" + id).session(session).with(csrf()).contentType("application/json")
                .content("{\"description\":\"Edited\",\"category\":\"Demo\",\"tags\":[\"test\"]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/videos/" + id + "/comments").session(session).with(csrf()).contentType("application/json")
                .content("{\"text\":\"Nice upload\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/videos/" + id)).andExpect(jsonPath("description").value("Edited"))
                .andExpect(jsonPath("comments[0].text").value("Nice upload"));
    }
}
