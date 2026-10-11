package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.dto.RegistrationRequest;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @InjectMocks RegistrationService service;

    @Test
    void hashesBeforeSaving() {
        when(encoder.encode("12345678")).thenReturn("encoded");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("visitor@example.com", service.register(
                new RegistrationRequest(" Visitor@Example.com ", "12345678")).email());
        verify(users).saveAndFlush(argThat(user -> "encoded".equals(user.getPasswordHash())));
    }

    @Test
    void preexistingEmailNeverHashesOrSaves() {
        when(users.existsByEmail("visitor@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> service.register(
                new RegistrationRequest("visitor@example.com", "12345678")));
        verifyNoInteractions(encoder);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentDuplicateConstraintOnly() {
        when(encoder.encode(any())).thenReturn("encoded");
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("constraint",
                new ConstraintViolationException("constraint", new SQLException(), "uk_app_users_email")));
        assertThrows(DuplicateEmailException.class, () -> service.register(
                new RegistrationRequest("visitor@example.com", "12345678")));
    }

    @Test
    void propagatesOtherDatabaseFailures() {
        when(encoder.encode(any())).thenReturn("encoded");
        DataIntegrityViolationException failure = new DataIntegrityViolationException("other constraint",
                new ConstraintViolationException("constraint", new SQLException(), "other_constraint"));
        when(users.saveAndFlush(any())).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> service.register(
                new RegistrationRequest("visitor@example.com", "12345678"))));
    }
}
