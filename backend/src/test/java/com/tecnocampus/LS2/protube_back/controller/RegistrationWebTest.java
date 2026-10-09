package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.configuration.SecurityConfig;
import com.tecnocampus.LS2.protube_back.dto.RegistrationRequest;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.services.DuplicateEmailException;
import com.tecnocampus.LS2.protube_back.services.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP/security contract tests independent of Docker; persistence is tested separately on PostgreSQL. */
@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"pro_tube.store.dir=./", "pro_tube.frontend.origin=http://localhost:5173"})
class RegistrationWebTest {
    @Autowired MockMvc mvc;
    @MockBean RegistrationService registrations;

    @Test
    void registrationAuthenticatesSessionAndReturnsOnlySafeFields() throws Exception {
        when(registrations.register(any())).thenReturn(new RegistrationResponse(1L, "visitor@example.com"));
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content("{\"email\":\" Visitor@Example.com \",\"password\":\"12345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"id\":1,\"email\":\"visitor@example.com\"}", true)).andReturn();
        verify(registrations).register(argThat(request -> "visitor@example.com".equals(request.getEmail())));
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value("visitor@example.com"));
    }

    @Test
    void invalidFieldsDoNotReachServiceOrExposeRejectedPassword() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("fieldErrors.email").value("Enter a valid email address."))
                .andExpect(jsonPath("fieldErrors.password").value("Password must contain at least 8 characters."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("short"))));
        verifyNoInteractions(registrations);
    }

    @Test
    void missingFieldsDoNotReachService() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("fieldErrors.email").exists())
                .andExpect(jsonPath("fieldErrors.password").exists());
        verifyNoInteractions(registrations);
    }

    @Test
    void duplicateEmailHasClearErrorWithoutAuthenticatedSession() throws Exception {
        when(registrations.register(any(RegistrationRequest.class))).thenThrow(new DuplicateEmailException());
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"visitor@example.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("fieldErrors.email").value("This email is already registered.")).andReturn();
        mvc.perform(get("/api/auth/session").session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isNoContent());
    }

    @Test
    void requiresCsrfAndSuppliesTokenToVisitor() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(registrations);
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("token").isNotEmpty());
        mvc.perform(get("/api/auth/session")).andExpect(status().isNoContent());
    }

    @Test
    void realCsrfTokenWorksAndSessionIdChangesAfterRegistration() throws Exception {
        when(registrations.register(any())).thenReturn(new RegistrationResponse(1L, "visitor@example.com"));
        var csrfResult = mvc.perform(get("/api/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String originalId = session.getId();
        String token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(csrfResult.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/register").session(session).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("{\"email\":\"visitor@example.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isCreated());
        assertNotEquals(originalId, session.getId());
        mvc.perform(get("/api/auth/session").session(session)).andExpect(status().isOk());
    }

    @Test
    void allowsConfiguredCredentialedOriginAndRejectsOthers() throws Exception {
        mvc.perform(options("/api/auth/register").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/auth/register").header("Origin", "http://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}
