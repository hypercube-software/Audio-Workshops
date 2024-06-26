package com.hypercube.workshop.syntheditor.infra.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypercube.workshop.syntheditor.infra.bus.dto.ParameterUpdateDTO;
import com.hypercube.workshop.syntheditor.infra.bus.dto.SynthEditorMessageDTO;
import com.hypercube.workshop.syntheditor.infra.bus.dto.SynthEditorMessageType;
import com.hypercube.workshop.syntheditor.model.error.SynthEditorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bidirectional interface between browser and server
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketBus extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    @Autowired
    @Lazy
    private SynthEditorBusListener synthEditorBusListener;

    public void send(WebSocketSession session, SynthEditorMessageDTO msg) {
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
        session.close();
        synthEditorBusListener.onSessionClosed();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("afterConnectionEstablished: sessionId=" + session.getId());
        sessionMap.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ParameterUpdateDTO parameterUpdateDTO = objectMapper.readValue(message.getPayload(), ParameterUpdateDTO.class);
            synthEditorBusListener.onMsg(parameterUpdateDTO);
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }

    @Scheduled(fixedRate = 3000)
    public void task() {
        sessionMap.values()
                .forEach(session -> {
                    send(session, new SynthEditorMessageDTO(SynthEditorMessageType.INFO, "Hello from server"));
                });
    }

    public void sendProgress(int progress) {
        sessionMap.values()
                .forEach(s -> send(s, new SynthEditorMessageDTO(SynthEditorMessageType.PROGRESS, Integer.toString(progress))));
    }
}
