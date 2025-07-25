package com.example.vargoBackend.model;

import java.math.BigDecimal;
import java.time.Instant;

public record User(Long id, String steamId, String username, BigDecimal balance, Instant createdAt) {}