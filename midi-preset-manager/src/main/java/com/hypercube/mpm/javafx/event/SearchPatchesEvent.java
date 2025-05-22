package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

@Getter
public class SearchPatchesEvent extends Event {
    private final String searchText;

    public SearchPatchesEvent(Object view, EventTarget target, EventType<?> eventType, String searchText) {
        super(view, target, eventType);
        this.searchText = searchText;
    }
}
