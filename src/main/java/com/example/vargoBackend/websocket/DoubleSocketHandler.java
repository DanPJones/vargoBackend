package com.example.vargoBackend.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nonnull;

@Component
public class DoubleSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public DoubleSocketHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession s) {
        sessions.add(s);
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession s, @Nonnull CloseStatus status) {
        sessions.remove(s);
    }

    // @Override
    // protected void handleTextMessage(WebSocketSession s, TextMessage msgTxt) throws IOException {

    // }

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
