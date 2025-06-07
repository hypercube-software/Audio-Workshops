package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

import java.util.List;

@Getter
public class SelectionChangedEvent extends Event {
    private final String dataSource;
    private final List<Integer> selectedIndexes;
    private final List<Object> selectedItems;

    public SelectionChangedEvent(Object view, EventTarget target, EventType<?> eventType, String dataSource, List<Integer> selectedIndexes, List<Object> selectedItems) {
        super(view, target, eventType);
        this.dataSource = dataSource;
        this.selectedIndexes = selectedIndexes;
        this.selectedItems = selectedItems;
    }
}
