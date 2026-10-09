package com.tecnocampus.LS2.protube_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public final class RegistrationRequest {
    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    private final String email;

    @NotNull(message = "Password is required.")
    @Size(min = 8, message = "Password must contain at least 8 characters.")
    private final String password;

    @com.fasterxml.jackson.annotation.JsonCreator
    public RegistrationRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("email") String email,
            @com.fasterxml.jackson.annotation.JsonProperty("password") String password) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
