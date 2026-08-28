package com.example.analytics.controller;

import com.example.analytics.dto.Alert;
import com.example.analytics.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/api/alerts")
    public List<Alert> getAlerts() {
        return alertService.getActiveAlerts();
    }
}
