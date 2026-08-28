package com.example.analytics.controller;

import com.example.analytics.service.SimulatorControlService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {
    private final SimulatorControlService control;

    public SimulatorController(SimulatorControlService control) {
        this.control = control;
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("paused", control.isPaused());
    }

    @PostMapping("/pause")
    public Map<String, Boolean> pause() {
        control.pause();
        return status();
    }

    @PostMapping("/resume")
    public Map<String, Boolean> resume() {
        control.resume();
        return status();
    }
}
