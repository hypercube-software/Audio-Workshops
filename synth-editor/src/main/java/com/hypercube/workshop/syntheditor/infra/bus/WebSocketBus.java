package com.hypercube.workshop.syntheditor.infra.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypercube.workshop.syntheditor.infra.bus.dto.ParameterUpdateDTO;
import com.hypercube.workshop.syntheditor.infra.bus.dto.SynthEditorMessageDTO;
import com.hypercube.workshop.syntheditor.model.error.SynthEditorException;
import com.hypercube.workshop.syntheditor.service.SynthEditorService;
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

/**
 * Bidirectional interface between browser and server
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketBus extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final SynthEditorService synthEditorService;
    Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

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
        try {
            ParameterUpdateDTO parameterUpdateDTO = objectMapper.readValue(message.getPayload(), ParameterUpdateDTO.class);
            synthEditorService.onMsg(parameterUpdateDTO);
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }

    @Scheduled(fixedRate = 3000)
    public void task() {
        sessionMap.values()
                .forEach(session -> {
                    send(session, new SynthEditorMessageDTO("Hello from server"));
                });
    }
}
