package com.example.vargoBackend.service;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.vargoBackend.websocket.DoubleSocketHandler;

import jakarta.annotation.PostConstruct;

@Service
@EnableScheduling
public class RoundGenerator {

    private static final long ROUND_MS = 10_000;
    private static final long BET_LOCK_MS = 5_000;
    private static boolean spun = false;
    private final DoubleSocketHandler socket;   // injected

    public RoundGenerator(DoubleSocketHandler socket) {
        this.socket = socket;
    }

    public record Round(long rollTime,
            int rollPx,
            int winnerTile) {

    }

    private final AtomicReference<Round> nextRound = new AtomicReference<>();

    @PostConstruct
    private void init() {
        scheduleNext();
    }

    @Scheduled(fixedRate = ROUND_MS)
    private void scheduleNext() {

        Round previous = nextRound.get();
        if (previous != null) {
            triggerSpin(previous.rollPx, previous.rollTime);
        }


        long rollTime = System.currentTimeMillis() + ROUND_MS;
        int randomInt = ThreadLocalRandom.current().nextInt(3600);
        int rollPx = (randomInt + 4200) * (-1);
        int winnerTile = (randomInt % 1200) / 80;
        nextRound.set(new Round(rollTime, rollPx, winnerTile));
    }

    public Round current() {
        return nextRound.get();
    }

    public boolean betsOpen() {
        long now = System.currentTimeMillis();
        return now < current().rollTime() - BET_LOCK_MS;
    }

    public void triggerSpin(int rollPx, long rollTime) {
        try {
            socket.broadcast(new DoubleSocketHandler.OutMsg.Spin(rollPx, rollTime));
        } catch (IOException e) {
        }
    }
}
