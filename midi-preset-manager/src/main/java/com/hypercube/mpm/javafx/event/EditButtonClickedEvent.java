package com.hypercube.mpm.javafx.event;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

@Getter
public class EditButtonClickedEvent extends ActionEvent {
    private final String attributeSelectorId;
    private final String buttonId;

    public EditButtonClickedEvent(Object view, EventTarget target, EventType<? extends Event> eventType, String attributeSelectorId, String buttonId) {
        super(view, target);
        this.attributeSelectorId = attributeSelectorId;
        this.buttonId = buttonId;
    }
}
