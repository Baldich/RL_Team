package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.dto.ProfileUpdateRequest;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ProfileService service = new ProfileService(users);

    @Test
    void existingUsersHaveEmptyPublicFieldsAndCanUpdateThem() {
        User user = new User("private@example.com", "private-hash");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.saveAndFlush(user)).thenReturn(user);
        var initial = service.getProfile(7L);
        assertEquals("", initial.displayName());
        assertEquals("", initial.avatarUrl());
        assertEquals("", initial.description());
        var updated = service.updateProfile(7L, new ProfileUpdateRequest(" Viewer ", " https://example.com/a.png ", " About "));
        assertEquals("Viewer", updated.displayName());
        assertEquals("https://example.com/a.png", updated.avatarUrl());
        assertEquals("About", updated.description());
        assertEquals("private@example.com", user.getEmail());
        assertEquals("private-hash", user.getPasswordHash());
        verify(users).saveAndFlush(user);
    }

    @Test
    void missingProfilesReturnNotFound() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.getProfile(99L)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> service.updateProfile(99L, new ProfileUpdateRequest("", "", "")));
        verify(users, never()).saveAndFlush(any());
    }
}
