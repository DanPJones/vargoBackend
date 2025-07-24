package com.example.vargoBackend.service;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.vargoBackend.repo.UserRepository;
import com.example.vargoBackend.websocket.DoubleSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
@EnableScheduling
public class RoundGenerator {

    private static final int[] tiles = {11, 5, 10, 6, 9, 7, 8, 1, 14, 2, 13, 3, 12, 4, 0};
    private static final Set<Integer> RED_TILES = Set.of(5, 6, 7, 1, 2, 3, 4);
    private static final Set<Integer> BLACK_TILES = Set.of(8, 9, 10, 11, 12, 13, 14);
    private static final long ROUND_MS = 15_000;
    private static final long BET_LOCK_MS = 500;
    private static final AtomicLong redTotal = new AtomicLong();
    private static final AtomicLong greenTotal = new AtomicLong();
    private static final AtomicLong blackTotal = new AtomicLong();
    private static final long BET_UPDATE_MS = 2_000;
    private final DoubleSocketHandler socket;
    private final UserRepository userRepo;
    private final TaskScheduler scheduler;
    private long roundId = 0;
    private static final List<Bet> allBets = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper;
    private final Deque<Integer> resBalls = new ArrayDeque<>(10);

    public record Bet(long steamId, String color, long amount, WebSocketSession s, long roundId) {

    }

    ;

    public class TotalBetError extends RuntimeException {

        public TotalBetError() {
            super("Unknown Total Bet Color");
        }
    }

    public RoundGenerator(DoubleSocketHandler socket, UserRepository userRepo, TaskScheduler scheduler, ObjectMapper mapper) {
        this.socket = socket;
        this.userRepo = userRepo;
        this.scheduler = scheduler;
        this.mapper = mapper;
    }

    public long getRoundId() {
        return roundId;
    }

    public record Round(long rollTime,
            int rollPx,
            int winnerTile,
            long roundId) {

    }

    private final AtomicReference<Round> nextRound = new AtomicReference<>();

    @PostConstruct
    private void init() {
        scheduleNext();
        updateBets();

    }

    @Scheduled(fixedRate = ROUND_MS)
    private void scheduleNext() {
        this.roundId++;
        Round previous = nextRound.get();
        if (previous != null) {
            triggerSpin(previous.rollPx, previous.rollTime);
            scheduler.schedule(() -> settleRound(previous), Instant.now().plusMillis(7500));
            scheduler.schedule(() -> broadCastBetsClosed(), Instant.now().plusMillis(ROUND_MS - BET_LOCK_MS));
        }
        long rollTime = System.currentTimeMillis() + ROUND_MS;
        int randomInt = ThreadLocalRandom.current().nextInt(3600);
        int rollPx = (randomInt + 4200) * (-1);
        int winnerTile = (randomInt % 1200) / 80;
        System.out.println("WINNER: " + (RED_TILES.contains(tiles[winnerTile]) == true ? "red " : BLACK_TILES.contains(tiles[winnerTile]) == true ? "black " : "green ") + tiles[winnerTile]);
        nextRound.set(new Round(rollTime, rollPx, winnerTile, this.roundId));
    }

    private void settleRound(Round round) {
        if (resBalls.size() == 10) {
            resBalls.removeFirst();
        }
        resBalls.addLast(tiles[round.winnerTile]);

        try {
            Map<String, Object> msg = Map.of(
                    "type", "updateResBalls",
                    "resBalls", this.resBalls
            );
            socket.broadcast(msg);
            System.out.println("updating resBalls?");
        } catch (IOException e) {
            System.out.print("anal");
        }

        redTotal.set(0);
        greenTotal.set(0);
        blackTotal.set(0);
        String winColor = RED_TILES.contains(tiles[round.winnerTile()]) == true ? "red" : BLACK_TILES.contains(tiles[round.winnerTile()]) == true ? "black" : "green";
        System.out.println("SETTLING BETS FOR: " + tiles[round.winnerTile]);
        int multiplier = (winColor.equals("red") || winColor.equals("black")) ? 2 : 14;

        for (Bet bet : allBets) {
            if (bet.color().equals(winColor) && bet.roundId() == round.roundId()) {
                System.out.println("Bettor" + bet.steamId());
                BigInteger newBal = userRepo.creditBalance(bet.steamId(), BigInteger.valueOf(bet.amount()), multiplier);

                Map<String, Object> payload = Map.of(
                        "type", "balanceWin",
                        "balance", newBal
                );
                try {
                    String json = mapper.writeValueAsString(payload);
                    bet.s().sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    System.out.println("error sending betWin");
                }
                allBets.remove(bet);
            } else {
                Map<String, Object> payload = Map.of(
                        "type", "balanceLoss"
                );
                try {
                    String json = mapper.writeValueAsString(payload);
                    bet.s().sendMessage(new TextMessage(json));
                } catch (Exception e) {
                }
            }
        }
        broadCastBetsOpen();
    }

    @Scheduled(fixedRate = BET_UPDATE_MS)
    private void updateBets() {
        try {
            Map<String, Object> msg = Map.of(
                    "type", "totalBetUpdate",
                    "red", redTotal.get(),
                    "green", greenTotal.get(),
                    "black", blackTotal.get()
            );
            socket.broadcast(msg);
        } catch (IOException e) {
            System.out.print("anal");
        }

    }

    private void broadCastBetsOpen() {
        try {
            Map<String, Object> msg = Map.of(
                    "type", "betsOpen"
            );
            socket.broadcast(msg);
        } catch (IOException e) {
            System.out.print("anal");
        }
    }

    private void broadCastBetsClosed() {
        try {
            Map<String, Object> msg = Map.of(
                    "type", "betsClosed"
            );
            socket.broadcast(msg);
        } catch (IOException e) {
            System.out.print("anal");
        }
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

    public BigInteger placeBet(Bet bet, String steamId) throws Exception {
        if (!betsOpen()) {
            throw new Exception("bets closed");
        }
        if (bet.amount() < 1) {
            throw new Exception("bet amount must be > 0");
        }

        BigInteger balance = userRepo.getUserBalance(bet.steamId());
        BigInteger wager = BigInteger.valueOf(bet.amount());

        if (balance.compareTo(wager) < 0) {
            throw new Exception("insufficient balance");
        }

        BigInteger resultBalance = userRepo.debitBalance(bet.steamId(), wager);
        allBets.add(bet);
        // put bet into the book

        switch (bet.color()) {
            case "red" ->
                redTotal.addAndGet(bet.amount());
            case "black" ->
                blackTotal.addAndGet(bet.amount());
            case "green" ->
                greenTotal.addAndGet(bet.amount());
            default ->
                throw new TotalBetError();

        }

        return resultBalance;

    }
}
