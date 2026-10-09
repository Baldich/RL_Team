package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.dto.RegistrationRequest;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        if (users.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }
        try {
            User user = users.saveAndFlush(new User(request.getEmail(), encoder.encode(request.getPassword())));
            return new RegistrationResponse(user.getId(), user.getEmail());
        } catch (DataIntegrityViolationException exception) {
            // Only the named email constraint represents a duplicate registration.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uk_app_users_email".equals(violation.getConstraintName())) {
                    throw new DuplicateEmailException();
                }
            }
            throw exception;
        }
    }
}
