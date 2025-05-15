package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

@Getter
public class DeviceChangedEvent extends Event {
    private final int deviceIndex;

    public DeviceChangedEvent(Object view, EventTarget target, EventType<?> eventType, int deviceIndex) {
        super(view, target, eventType);
        this.deviceIndex = deviceIndex;
    }
}
