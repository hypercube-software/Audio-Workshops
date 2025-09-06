package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

@Getter
public class MuteOutputDeviceEvent extends Event {
    private final String device;
    private final boolean mute;

    public MuteOutputDeviceEvent(Object view, EventTarget target, EventType<? extends Event> eventType, String device, boolean mute) {
        super(view, target, eventType);
        this.device = device;
        this.mute = mute;
    }
}
