package com.example.vargoBackend.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.vargoBackend.websocket.DoubleSocketHandler;

import jakarta.annotation.PostConstruct;

@Service
@EnableScheduling
public class BetService {

    private static AtomicLong redTotal = new AtomicLong();
    private static AtomicLong greenTotal = new AtomicLong();
    private static AtomicLong blackTotal = new AtomicLong();
    private static final long BET_UPDATE_MS = 2_000;
    private final DoubleSocketHandler socket;

    public BetService(DoubleSocketHandler socket) {
        this.socket = socket;
    }

    @PostConstruct
    private void init() {
        updateBets();
    }

    @Scheduled(fixedRate = BET_UPDATE_MS)
    private void updateBets() {
        try {
            Map<String, Object> msg = Map.of(
                    "type", "betUpdate",
                    "red", redTotal,
                    "green", greenTotal,
                    "black", blackTotal
            );
            socket.broadcast(msg);
        } catch (IOException e) {
            System.out.print("anal");
        }

    }

}
