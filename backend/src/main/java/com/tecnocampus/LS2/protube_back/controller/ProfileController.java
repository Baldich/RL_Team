package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.ApiError;
import com.tecnocampus.LS2.protube_back.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.dto.ProfileUpdateRequest;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/users/{id}/profile")
    public ProfileResponse publicProfile(@PathVariable long id) {
        return profiles.getProfile(id);
    }

    @GetMapping("/profile")
    public ProfileResponse ownProfile(@AuthenticationPrincipal RegistrationResponse user) {
        return profiles.getProfile(authenticatedId(user));
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal RegistrationResponse user,
                                         @Valid @RequestBody ProfileUpdateRequest request) {
        // The target account always comes from the session, never from submitted data.
        return profiles.updateProfile(authenticatedId(user), request);
    }

    private static long authenticatedId(RegistrationResponse user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user.id();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidProfile(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("Check the profile fields.", errors));
    }
}
