package com.example.vargoBackend.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * DEV in‑memory session storage.
 * Maps session-token -> userId.
 * Replace w/ JWT or persistent store later.
 */
@Component
public class SessionStore {

    private final ConcurrentMap<String, Long> sessions = new ConcurrentHashMap<>();

    public void put(String token, Long userId) {
        sessions.put(token, userId);
    }

    public Long get(String token) {
        return sessions.get(token);
    }

    public void remove(String token) {
        sessions.remove(token);
    }
}