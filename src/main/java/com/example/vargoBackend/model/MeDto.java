package com.example.vargoBackend.model;

import java.time.Instant;

public record MeDto(
    Long id,
    String steamId,
    String username,
    String balance,   
    Instant createdAt
) {}