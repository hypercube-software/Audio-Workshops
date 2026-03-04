package com.hypercube.mpm.javafx.event;

import com.hypercube.mpm.model.Patch;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

@Getter
public class PatchScoreChangedEvent extends Event {
    private final Patch patch;

    public PatchScoreChangedEvent(Object view, EventTarget target, EventType<?> eventType, Patch patch) {
        super(view, target, eventType);
        this.patch = patch;
    }
}
