package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.RegistrationRequest;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final RegistrationService registrations;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;

    public RegistrationController(RegistrationService registrations, SecurityContextRepository contexts,
                                  CsrfTokenRepository csrfTokens) {
        this.registrations = registrations;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest registration,
            HttpServletRequest request, HttpServletResponse response) {
        RegistrationResponse user = registrations.register(registration);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
        new CsrfAuthenticationStrategy(csrfTokens).onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return ResponseEntity.status(201).body(user);
    }

    @GetMapping("/session")
    public ResponseEntity<RegistrationResponse> session(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof RegistrationResponse user) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    public record CsrfResponse(String headerName, String token) {}
}
