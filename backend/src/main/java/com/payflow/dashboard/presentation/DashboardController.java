package com.payflow.dashboard.presentation;

import java.util.UUID;

import com.payflow.dashboard.application.DashboardQueries;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardQueries dashboard;

    public DashboardController(DashboardQueries dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    DashboardQueries.DashboardSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return dashboard.summary(UUID.fromString(jwt.getSubject()));
    }
}
