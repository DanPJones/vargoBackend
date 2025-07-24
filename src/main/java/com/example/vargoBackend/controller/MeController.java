package com.example.vargoBackend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vargoBackend.auth.SessionStore;
import com.example.vargoBackend.repo.UserRepository;



@RestController
@RequestMapping("/api")
public class MeController {

    private final SessionStore sessionStore;
    private final UserRepository userRepo;

    public MeController(SessionStore sessionStore, UserRepository userRepo) {
        this.sessionStore = sessionStore;
        this.userRepo = userRepo;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@CookieValue(name = "session", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).body("No session.");
        }

        Long userId = sessionStore.get(token);
        if (userId == null) {
            return ResponseEntity.status(401).body("Invalid session.");
        }

    return userRepo.findById(userId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found.")));
    }

}
