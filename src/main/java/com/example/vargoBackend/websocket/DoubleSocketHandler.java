package com.example.vargoBackend.websocket;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.vargoBackend.service.RoundGenerator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.vargoBackend.service.RoundGenerator.Bet;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Nonnull;

@Component
public class DoubleSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final RoundGenerator roundGenerator;

    record BetMsg(String type, String color, long amount) {

    }

    public DoubleSocketHandler(ObjectMapper mapper, @Lazy RoundGenerator roundGenerator) {
        this.mapper = mapper;
        this.roundGenerator = roundGenerator;
    }

    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession s) {
        sessions.add(s);
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession s, @Nonnull CloseStatus status) {
        sessions.remove(s);
    }

    @Override
    protected void handleTextMessage(WebSocketSession s, TextMessage msgTxt) throws IOException {
        String steamId = (String) s.getAttributes().get("steamId");
        BigInteger resultBalance = null;

        JsonNode root = mapper.readTree(msgTxt.getPayload());
        String type = root.path("type").asText();

        switch (type) {
            case "bet" -> {
                BetMsg msg = mapper.readValue(msgTxt.getPayload(), BetMsg.class);

                Bet bet = new Bet(Long.parseLong(steamId), msg.color(), msg.amount(), s, roundGenerator.getRoundId());

                String payloada = msgTxt.getPayload();
                System.out.println("Incoming WS payload: " + payloada);

                try {
                    resultBalance = roundGenerator.placeBet(bet, steamId);
                } catch (Exception e) {
                    System.out.println("bet failed: " + e.getMessage());
                }

                try {
                    if (resultBalance != null) {
                        Map<String, Object> payload = Map.of(
                                "type", "balance",
                                "balance", resultBalance,
                                "color", msg.color(),
                                "amount", msg.amount()
                        );
                        String json = mapper.writeValueAsString(payload);
                        s.sendMessage(new TextMessage(json));
                    }

                } catch (Exception e) {
                    System.out.println("Error sending back balance " + e.getMessage());
                }
            }
            case "chat" -> {

            }
        }

        System.out.println(steamId);
    }

    public sealed interface OutMsg permits OutMsg.Spin {

        record Spin(int rollPx, long rollTime) implements OutMsg {

            @JsonProperty
            public String type() {
                return "spin";
            }
        }
    }

    public void broadcast(Object payload) throws IOException {
        String json = (payload instanceof String s)
                ? s
                : mapper.writeValueAsString(payload);

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

}
