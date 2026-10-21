package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.dto.ProfileUpdateRequest;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {
    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(long id) {
        return response(findUser(id));
    }

    @Transactional
    public ProfileResponse updateProfile(long id, ProfileUpdateRequest request) {
        User user = findUser(id);
        user.updateProfile(request.displayName(), request.avatarUrl(), request.description());
        return response(users.saveAndFlush(user));
    }

    private User findUser(long id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found."));
    }

    private static ProfileResponse response(User user) {
        return new ProfileResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl(), user.getDescription());
    }
}
