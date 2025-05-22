package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

import java.util.List;

@Getter
public class SelectionChangedEvent extends Event {
    private final String dataSource;
    private final List<Integer> selectedItems;

    public SelectionChangedEvent(Object view, EventTarget target, EventType<?> eventType, String dataSource, List<Integer> selectedItems) {
        super(view, target, eventType);
        this.dataSource = dataSource;
        this.selectedItems = selectedItems;
    }
}
