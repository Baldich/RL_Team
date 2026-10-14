package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.configuration.SecurityConfig;
import com.tecnocampus.LS2.protube_back.dto.*;
import com.tecnocampus.LS2.protube_back.services.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideosController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"pro_tube.store.dir=./", "pro_tube.frontend.origin=http://localhost:5173"})
class VideosControllerTest {
    @Autowired MockMvc mvc;
    @MockBean VideoService videos;

    @Test
    void visitorsAndAuthenticatedUsersReceiveOnlyPublicSummaries() throws Exception {
        when(videos.getVideos(0, 12)).thenReturn(new VideoPageResponse(List.of(new VideoSummaryResponse(1L, "Title", "0.webp")), 0, 12, 1, 1));
        String expected = "{\"content\":[{\"id\":1,\"title\":\"Title\",\"thumbnailPath\":\"0.webp\"}],\"page\":0,\"size\":12,\"totalElements\":1,\"totalPages\":1}";
        mvc.perform(get("/api/videos")).andExpect(status().isOk()).andExpect(content().json(expected, true));
        mvc.perform(get("/api/videos").with(user("viewer"))).andExpect(status().isOk()).andExpect(content().json(expected, true));
    }
    @Test
    void acceptsRequestedAndEmptyPages() throws Exception {
        when(videos.getVideos(2, 5)).thenReturn(new VideoPageResponse(List.of(), 2, 5, 0, 0));
        mvc.perform(get("/api/videos?page=2&size=5")).andExpect(status().isOk())
                .andExpect(jsonPath("content").isEmpty()).andExpect(jsonPath("page").value(2));
        verify(videos).getVideos(2, 5);
    }
    @Test
    void rejectsInvalidPagination() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=-1", "size=101", "page=abc")) {
            mvc.perform(get("/api/videos?" + query)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(videos);
    }
}
