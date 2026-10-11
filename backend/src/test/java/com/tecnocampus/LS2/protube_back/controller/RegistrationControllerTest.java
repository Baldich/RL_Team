package com.tecnocampus.LS2.protube_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.PostgresTestSupport;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"pro_tube.store.dir=./", "pro_tube.load_initial_data=false"})
@AutoConfigureMockMvc
class RegistrationControllerTest extends PostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    @BeforeEach
    void clearUsers() { users.deleteAll(); }

    private String body(String email, String password) throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("email", email);
        values.put("password", password);
        return json.writeValueAsString(values);
    }

    private MvcResult register(String email, String password, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content(body(email, password)))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    @Test
    void validRegistrationPersistsHashAndAuthenticatesFollowingRequest() throws Exception {
        MvcResult result = register("  Visitor@EXAMPLE.com  ", "12345678", 201);
        assertEquals(1, users.count());
        User stored = users.findAll().getFirst();
        assertEquals("visitor@example.com", stored.getEmail());
        assertNotEquals("12345678", stored.getPasswordHash());
        assertTrue(encoder.matches("12345678", stored.getPasswordHash()));
        var response = json.readTree(result.getResponse().getContentAsString());
        assertEquals(2, response.size());
        assertEquals(stored.getId().longValue(), response.get("id").asLong());
        assertFalse(result.getResponse().getContentAsString().contains(stored.getPasswordHash()));
        assertFalse(result.getResponse().getContentAsString().contains("12345678"));
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        assertNotNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value("visitor@example.com"));
    }

    @Test
    void duplicateNormalizedEmailDoesNotCreateOrAuthenticateAnotherUser() throws Exception {
        register("visitor@example.com", "12345678", 201);
        MvcResult duplicate = register("  VISITOR@EXAMPLE.COM ", "abcdefgh", 409);
        assertEquals(1, users.count());
        assertEquals("This email is already registered.",
                json.readTree(duplicate.getResponse().getContentAsString()).get("fieldErrors").get("email").asText());
        assertNull(duplicate.getRequest().getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "@example.com", "a@", "a b@example.com"})
    void invalidEmailCreatesNoAccount(String email) throws Exception {
        MvcResult result = register(email, "12345678", 400);
        assertTrue(json.readTree(result.getResponse().getContentAsString()).get("fieldErrors").has("email"));
        assertEquals(0, users.count());
        assertNull(result.getRequest().getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", "1234567"})
    void shortOrMissingPasswordCreatesNoAccount(String password) throws Exception {
        MvcResult result = register("visitor@example.com", password, 400);
        assertTrue(json.readTree(result.getResponse().getContentAsString()).get("fieldErrors").has("password"));
        assertFalse(result.getResponse().getContentAsString().contains("rejectedValue"));
        assertEquals(0, users.count());
    }

    @Test
    void databaseEnforcesUniqueEmail() {
        users.saveAndFlush(new User("visitor@example.com", encoder.encode("12345678")));
        assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(new User("visitor@example.com", encoder.encode("abcdefgh"))));
        assertEquals(1, users.count());
    }

    @Test
    void csrfTokenIsRequiredAndAnonymousEndpointsStayPublic() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(body("visitor@example.com", "12345678"))).andExpect(status().isForbidden());
        assertEquals(0, users.count());
        mvc.perform(get("/api/auth/session")).andExpect(status().isNoContent());
        mvc.perform(get("/api/videos")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("token").isNotEmpty());
    }

    @Test
    void csrfEndpointTokenCanRegisterAndSessionIdIsRotated() throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String originalId = session.getId();
        String token = json.readTree(csrfResult.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/register").session(session).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content(body("visitor@example.com", "12345678")))
                .andExpect(status().isCreated());
        assertNotEquals(originalId, session.getId());
    }

    @Test
    void corsAllowsOnlyConfiguredFrontendWithCredentials() throws Exception {
        mvc.perform(options("/api/auth/register").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/register").header("Origin", "http://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}
