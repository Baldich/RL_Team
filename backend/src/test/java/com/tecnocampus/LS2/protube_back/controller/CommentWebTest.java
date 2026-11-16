package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.configuration.SecurityConfig;
import com.tecnocampus.LS2.protube_back.dto.*;
import com.tecnocampus.LS2.protube_back.services.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"pro_tube.store.dir=./", "pro_tube.frontend.origin=http://localhost:5173"})
class CommentWebTest {
    @Autowired MockMvc mvc;
    @MockBean CommentService comments;
    private static final String BASE = "/api/videos/1/comments/imported-0";

    private org.springframework.test.web.servlet.request.RequestPostProcessor viewer() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new RegistrationResponse(7L, "viewer@example.com"), null, List.of()));
    }

    @Test void repliesUseSessionIdentityAndReturnCreatedDto() throws Exception {
        when(comments.reply(1L, "imported-0", 7L, "Reply")).thenReturn(
                new VideoCommentResponse("local-12", "viewer@example.com", "Reply", 123, 0, false, List.of()));
        mvc.perform(post(BASE + "/replies").with(viewer()).with(csrf()).contentType("application/json")
                .content("{\"text\":\" Reply \",\"userId\":99}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("id").value("local-12"))
                .andExpect(jsonPath("replies").isEmpty());
        verify(comments).reply(1L, "imported-0", 7L, "Reply");
    }

    @Test void putAndDeleteReturnCanonicalLikeState() throws Exception {
        when(comments.setLiked(1L, "imported-0", 7L, true)).thenReturn(new CommentLikeResponse(5, true));
        when(comments.setLiked(1L, "imported-0", 7L, false)).thenReturn(new CommentLikeResponse(4, false));
        mvc.perform(put(BASE + "/like").with(viewer()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("likeCount").value(5))
                .andExpect(jsonPath("likedByCurrentUser").value(true));
        mvc.perform(delete(BASE + "/like").with(viewer()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("likeCount").value(4))
                .andExpect(jsonPath("likedByCurrentUser").value(false));
    }

    private MockHttpServletRequestBuilder write(String method) {
        return switch (method) {
            case "POST" -> post(BASE + "/replies").contentType("application/json").content("{\"text\":\"Reply\"}");
            case "PUT" -> put(BASE + "/like");
            default -> delete(BASE + "/like");
        };
    }

    @Test void everyWriteRequiresAuthenticationCsrfAndRecognizedPrincipal() throws Exception {
        for (String method : List.of("POST", "PUT", "DELETE")) {
            mvc.perform(write(method).with(csrf())).andExpect(status().isUnauthorized());
            mvc.perform(write(method).with(viewer())).andExpect(status().isForbidden());
            mvc.perform(write(method).with(user("unexpected-principal")).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(comments);
    }

    @Test void validatesMissingBlankAndOversizedReplies() throws Exception {
        for (String body : List.of("{}", "{\"text\":\"   \"}", "{\"text\":\"" + "x".repeat(5001) + "\"}")) {
            mvc.perform(post(BASE + "/replies").with(viewer()).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("fieldErrors.text").exists());
        }
        verifyNoInteractions(comments);
    }

    @Test void missingTargetsReturnNotFoundAndCorsAllowsCredentialedDelete() throws Exception {
        when(comments.setLiked(1L, "imported-0", 7L, true))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(put(BASE + "/like").with(viewer()).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(options(BASE + "/like").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Access-Control-Request-Headers", "X-CSRF-TOKEN"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options(BASE + "/like").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "DELETE")).andExpect(status().isForbidden());
    }
}
