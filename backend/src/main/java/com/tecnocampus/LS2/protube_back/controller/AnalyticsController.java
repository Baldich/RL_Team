package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.AnalyticsResponse;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.services.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping
    public AnalyticsResponse getAnalytics(@AuthenticationPrincipal RegistrationResponse user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to view analytics.");
        }
        return analytics.getForUser(user.id());
    }
}
