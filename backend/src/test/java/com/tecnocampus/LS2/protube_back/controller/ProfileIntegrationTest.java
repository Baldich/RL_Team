package com.tecnocampus.LS2.protube_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.PostgresTestSupport;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"pro_tube.store.dir=./", "pro_tube.load_initial_data=false"})
@AutoConfigureMockMvc
class ProfileIntegrationTest extends PostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ObjectMapper json;
    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUsers() {
        createdIds.forEach(users::deleteById);
    }

    @Test
    void registeredUserUpdatesPersistentProfileWithoutChangingAnotherAccount() throws Exception {
        var registration = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"profile-owner@example.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(registration.getResponse().getContentAsString()).get("id").longValue();
        createdIds.add(id);
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        User other = users.saveAndFlush(new User("other-profile@example.com", "untouched-password-hash"));
        createdIds.add(other.getId());

        mvc.perform(get("/api/profile").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("displayName").value(""))
                .andExpect(jsonPath("avatarUrl").value(""))
                .andExpect(jsonPath("description").value(""));

        var token = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        String csrfToken = json.readTree(token.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(put("/api/profile").session(session).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json")
                .content("{\"id\":" + other.getId() + ",\"displayName\":\" Viewer \",\"avatarUrl\":\"https://example.com/a.png\",\"description\":\" About me \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(id))
                .andExpect(jsonPath("displayName").value("Viewer"));

        User persisted = users.findById(id).orElseThrow();
        assertEquals("Viewer", persisted.getDisplayName());
        assertEquals("About me", persisted.getDescription());
        assertEquals("", users.findById(other.getId()).orElseThrow().getDisplayName());
        mvc.perform(get("/api/users/" + id + "/profile")).andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(java.util.Map.of(
                        "id", id, "displayName", "Viewer", "avatarUrl", "https://example.com/a.png", "description", "About me")), true));
        mvc.perform(get("/api/profile").session(session)).andExpect(jsonPath("description").value("About me"));

        mvc.perform(put("/api/profile").session(session).with(csrf()).contentType("application/json")
                .content("{\"displayName\":\"Rejected\",\"avatarUrl\":\"javascript:alert(1)\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
        assertEquals("Viewer", users.findById(id).orElseThrow().getDisplayName());

        mvc.perform(put("/api/profile").session(session).with(csrf()).contentType("application/json")
                .content("{\"displayName\":\"\",\"avatarUrl\":\"\",\"description\":\"\"}"))
                .andExpect(status().isOk());
        assertEquals("", users.findById(id).orElseThrow().getAvatarUrl());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/" + id + "/profile")).andExpect(status().isOk());
    }
}
