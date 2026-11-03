package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.configuration.SecurityConfig;
import com.tecnocampus.LS2.protube_back.dto.*;
import com.tecnocampus.LS2.protube_back.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoUploadController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"pro_tube.store.dir=./", "pro_tube.frontend.origin=http://localhost:5173"})
class VideoUploadWebTest {
    @Autowired MockMvc mvc;
    @MockBean VideoUploadService uploads;
    MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1});
    MockMultipartFile metadata(String body) { return new MockMultipartFile("metadata", "", "application/json", body.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    org.springframework.test.web.servlet.request.RequestPostProcessor owner() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(new RegistrationResponse(7L, "owner@example.com"), null, List.of()));
    }
    @Test void createsUsingSessionOwnerAndReturnsLocation() throws Exception {
        when(uploads.upload(eq(7L), any(), any())).thenReturn(new VideoDetailResponse(42L, "Title", "Description", "a.mp4", "a.webp", List.of(), List.of(), "", 7L));
        mvc.perform(multipart("/api/videos").file(file).file(metadata("{\"title\":\" Title \",\"description\":\" Description \",\"ownerId\":99}"))
                .with(owner()).with(csrf())).andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/videos/42")).andExpect(jsonPath("ownerId").value(7));
        verify(uploads).upload(eq(7L), any(), eq(new VideoUploadRequest("Title", "Description")));
    }
    @Test void requiresSessionAndCsrf() throws Exception {
        mvc.perform(multipart("/api/videos").file(file).file(metadata("{}"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/videos").file(file).file(metadata("{}" )).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/videos").file(file).file(metadata("{}" )).with(owner())).andExpect(status().isForbidden());
        verifyNoInteractions(uploads);
    }
    @Test void validatesBlankAndLongMetadataAndMissingParts() throws Exception {
        for (String body : List.of("{}", "{\"title\":\" \",\"description\":\" \"}",
                "{\"title\":\"" + "a".repeat(256) + "\",\"description\":\"" + "b".repeat(5001) + "\"}")) {
            mvc.perform(multipart("/api/videos").file(file).file(metadata(body)).with(owner()).with(csrf()))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("fieldErrors.title").exists());
        }
        mvc.perform(multipart("/api/videos").file(file).with(owner()).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/videos").file(file).file(metadata("broken")).with(owner()).with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(uploads);
    }
    @Test void mapsProcessingAndSizeFailures() throws Exception {
        for (HttpStatus status : List.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, HttpStatus.PAYLOAD_TOO_LARGE,
                HttpStatus.UNPROCESSABLE_ENTITY, HttpStatus.INTERNAL_SERVER_ERROR)) {
            doThrow(new UploadException(status, "Upload failed")).when(uploads).upload(anyLong(), any(), any());
            mvc.perform(multipart("/api/videos").file(file).file(metadata("{\"title\":\"Title\",\"description\":\"Description\"}"))
                    .with(owner()).with(csrf())).andExpect(status().is(status.value())).andExpect(jsonPath("message").value("Upload failed"));
        }
        doThrow(new org.springframework.web.multipart.MaxUploadSizeExceededException(100)).when(uploads).upload(anyLong(), any(), any());
        mvc.perform(multipart("/api/videos").file(file).file(metadata("{\"title\":\"Title\",\"description\":\"Description\"}"))
                .with(owner()).with(csrf())).andExpect(status().isPayloadTooLarge());
    }
}
