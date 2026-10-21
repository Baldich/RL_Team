package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.configuration.SecurityConfig;
import com.tecnocampus.LS2.protube_back.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.services.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"pro_tube.store.dir=./", "pro_tube.frontend.origin=http://localhost:5173"})
class ProfileWebTest {
    @Autowired MockMvc mvc;
    @MockBean ProfileService profiles;

    private UsernamePasswordAuthenticationToken owner() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new RegistrationResponse(7L, "private@example.com"), null, List.of());
    }

    @Test
    void publicProfileContainsOnlyPublicFields() throws Exception {
        when(profiles.getProfile(7L)).thenReturn(new ProfileResponse(7L, "Viewer", "https://example.com/avatar.png", "About me"));
        mvc.perform(get("/api/users/7/profile")).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":7,"displayName":"Viewer","avatarUrl":"https://example.com/avatar.png","description":"About me"}
                        """, true));
    }

    @Test
    void readsOwnProfileUsingSessionIdentity() throws Exception {
        when(profiles.getProfile(7L)).thenReturn(new ProfileResponse(7L, "Viewer", "", ""));
        mvc.perform(get("/api/profile").with(authentication(owner())))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(7));
        verify(profiles).getProfile(7L);
    }

    @Test
    void anonymousVisitorsCannotReadOrUpdatePrivateEndpoint() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile").with(csrf()).contentType("application/json")
                .content("{\"displayName\":\"Other\",\"avatarUrl\":\"\",\"description\":\"\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test
    void unexpectedPrincipalIsRejected() throws Exception {
        mvc.perform(get("/api/profile").with(user("unrecognized-principal")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test
    void updatesOnlyAuthenticatedAccountEvenWhenPayloadSuppliesAnotherId() throws Exception {
        when(profiles.updateProfile(eq(7L), any())).thenReturn(new ProfileResponse(7L, "Updated", "", "About"));
        mvc.perform(put("/api/profile").with(authentication(owner())).with(csrf()).contentType("application/json")
                .content("{\"id\":99,\"displayName\":\" Updated \",\"avatarUrl\":\"\",\"description\":\" About \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(7));
        verify(profiles).updateProfile(eq(7L), argThat(fields ->
                fields.displayName().equals("Updated") && fields.description().equals("About")));
        verify(profiles, never()).updateProfile(eq(99L), any());
    }

    @Test
    void cannotPutToAnotherUsersPublicProfile() throws Exception {
        mvc.perform(put("/api/users/99/profile").with(authentication(owner())).with(csrf())
                .contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(profiles);
    }

    @Test
    void authenticatedUpdatesRequireCsrf() throws Exception {
        mvc.perform(put("/api/profile").with(authentication(owner())).contentType("application/json")
                .content("{\"displayName\":\"Viewer\",\"avatarUrl\":\"\",\"description\":\"\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsMissingFieldsUnsafeUrlsAndOversizedFields() throws Exception {
        mvc.perform(put("/api/profile").with(authentication(owner())).with(csrf())
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("fieldErrors.displayName").exists());
        for (String avatar : List.of("javascript:alert(1)", "data:image/png;base64,abc", "/relative.png", "https://bad host/a.png")) {
            mvc.perform(put("/api/profile").with(authentication(owner())).with(csrf()).contentType("application/json")
                    .content("{\"displayName\":\"Viewer\",\"avatarUrl\":\"" + avatar + "\",\"description\":\"\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("fieldErrors.avatarUrl").exists());
        }
        mvc.perform(put("/api/profile").with(authentication(owner())).with(csrf()).contentType("application/json")
                .content("{\"displayName\":\"" + "x".repeat(81) + "\",\"avatarUrl\":\"https://example.com/" + "a".repeat(2048)
                        + "\",\"description\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("fieldErrors.displayName").exists())
                .andExpect(jsonPath("fieldErrors.avatarUrl").exists())
                .andExpect(jsonPath("fieldErrors.description").exists());
        verifyNoInteractions(profiles);
    }

    @Test
    void missingPublicProfileReturnsNotFound() throws Exception {
        when(profiles.getProfile(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/users/99/profile")).andExpect(status().isNotFound());
    }

    @Test
    void corsAllowsCredentialedPutOnlyFromConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/profile").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/profile").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "PUT")).andExpect(status().isForbidden());
    }
}
