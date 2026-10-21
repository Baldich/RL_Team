package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.dto.LoginRequest;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public LoginService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public RegistrationResponse authenticate(LoginRequest request) {
        User user = users.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new RegistrationResponse(user.getId(), user.getEmail());
    }
}
