package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

@Getter
public class ScoreChangedEvent extends Event {
    private final int score;

    public ScoreChangedEvent(Object view, EventTarget target, EventType<?> eventType, int score) {
        super(view, target, eventType);
        this.score = score;
    }
}
