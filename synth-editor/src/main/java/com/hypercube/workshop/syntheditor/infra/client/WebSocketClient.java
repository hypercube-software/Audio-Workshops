package com.hypercube.workshop.syntheditor.infra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypercube.workshop.syntheditor.infra.client.dto.SynthEditorMessage;
import com.hypercube.workshop.syntheditor.model.SynthEditorService;
import com.hypercube.workshop.syntheditor.model.error.SynthEditorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketClient extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final SynthEditorService synthEditorService;
    Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public void send(WebSocketSession session, SynthEditorMessage msg) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        } catch (IOException e) {
            throw new SynthEditorException(e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("afterConnectionClosed: sessionId {} status {}", session.getId(), status.toString());
        sessionMap.remove(session.getId());
        synthEditorService.closeCurrentInputDevice();
        synthEditorService.closeCurrentOutputDevice();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("afterConnectionEstablished: sessionId=" + session.getId());
        sessionMap.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("handleTextMessage: sessionId=" + session.getId() + " Message=" + message.getPayload());
    }

    @Scheduled(fixedRate = 3000)
    public void task() {
        sessionMap.values()
                .forEach(session -> {
                    log.info("Send msg to client {}", session.getId());
                    send(session, new SynthEditorMessage("Hello from server"));
                });
    }
}
