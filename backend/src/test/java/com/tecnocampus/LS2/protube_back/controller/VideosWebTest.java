package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.PostgresTestSupport;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import com.tecnocampus.LS2.protube_back.services.VideoImportService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"pro_tube.load_initial_data=false", "ENV_PROTUBE_GOOGLE_CLIENT_ID=test", "ENV_PROTUBE_GOOGLE_CLIENT_SECRET=test"})
@AutoConfigureMockMvc
class VideosWebTest extends PostgresTestSupport {
    private static final Path STORE;
    static {
        try { STORE = Files.createTempDirectory("protube-us04-"); }
        catch (IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
    @DynamicPropertySource
    static void mediaStore(DynamicPropertyRegistry properties) {
        properties.add("pro_tube.store.dir", () -> STORE.toAbsolutePath().toString() + "/");
    }
    @Autowired MockMvc mvc;
    @Autowired VideoImportService importer;
    @Autowired VideoRepository videos;
    @Autowired com.tecnocampus.LS2.protube_back.repository.UserRepository users;
    private Long registeredId;

    @AfterEach
    void cleanCatalog() throws IOException {
        videos.deleteAll();
        if (registeredId != null) users.deleteById(registeredId);
        try (var files = Files.list(STORE)) {
            for (var file : files.toList()) Files.delete(file);
        }
    }
    @Test
    void grabberFilesBecomePersistentPublicPagesAndServedThumbnails() throws Exception {
        for (int i = 0; i < 13; i++) {
            Files.writeString(STORE.resolve(i + ".json"), "{\"id\":" + i + ",\"title\":\"Video " + i + "\"}");
            Files.write(STORE.resolve(i + ".webp"), new byte[]{1, 2, 3});
            Files.write(STORE.resolve(i + ".mp4"), new byte[]{1});
        }
        importer.importVideos(STORE);
        importer.importVideos(STORE);
        assertEquals(13, videos.count());
        assertTrue(videos.existsBySourceId(0L));
        var first = mvc.perform(get("/api/videos")).andExpect(status().isOk())
                .andExpect(jsonPath("content.length()").value(12))
                .andExpect(jsonPath("totalElements").value(13))
                .andExpect(jsonPath("totalPages").value(2)).andReturn();
        String credentials = "{\"email\":\"catalog-viewer@example.com\",\"password\":\"12345678\"}";
        var registration = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(credentials))
                .andExpect(status().isCreated()).andReturn();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        registeredId = mapper.readTree(registration.getResponse().getContentAsString()).path("id").longValue();
        var registeredSession = (MockHttpSession) registration.getRequest().getSession(false);
        mvc.perform(get("/api/videos").session(registeredSession)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(registeredSession).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"catalog-viewer@example.com\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(credentials))
                .andExpect(status().isOk()).andReturn();
        var loginSession = (MockHttpSession) login.getRequest().getSession(false);
        var second = mvc.perform(get("/api/videos?page=1").session(loginSession)).andExpect(status().isOk())
                .andExpect(jsonPath("content.length()").value(1)).andReturn();
        var firstJson = mapper.readTree(first.getResponse().getContentAsString());
        var lastId = mapper.readTree(second.getResponse().getContentAsString()).path("content").get(0).path("id").longValue();
        for (var summary : firstJson.path("content")) {
            assertNotEquals(lastId, summary.path("id").longValue());
            assertTrue(summary.path("title").asText().startsWith("Video "));
        }
        var thumbnail = firstJson.path("content").get(0).path("thumbnailPath").asText();
        mvc.perform(get("/media/" + thumbnail)).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1, 2, 3}));
        mvc.perform(post("/api/auth/logout").session(loginSession).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/videos?page=2")).andExpect(status().isOk()).andExpect(jsonPath("content").isEmpty());
    }

}
