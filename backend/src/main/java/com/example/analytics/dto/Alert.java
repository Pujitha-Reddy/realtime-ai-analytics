package com.example.analytics.dto;

import java.time.Instant;

public record Alert(String id, String severity, String message, Instant timestamp) {}
