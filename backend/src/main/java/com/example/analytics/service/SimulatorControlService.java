package com.example.analytics.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SimulatorControlService {
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public boolean isPaused() { return paused.get(); }
    public void pause() { paused.set(true); }
    public void resume() { paused.set(false); }
}
