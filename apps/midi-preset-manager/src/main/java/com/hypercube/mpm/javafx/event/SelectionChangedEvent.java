package com.hypercube.mpm.javafx.event;

import com.hypercube.mpm.javafx.error.ApplicationError;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

import java.util.List;

@Getter
public class SelectionChangedEvent<T> extends Event {
    private final String widgetId;
    private final List<Integer> selectedIndexes;
    private final List<T> selectedItems;

    public SelectionChangedEvent(Object view, EventTarget target, EventType<?> eventType, String widgetId, List<Integer> selectedIndexes, List<T> selectedItems) {
        super(view, target, eventType);
        this.widgetId = widgetId;
        this.selectedIndexes = selectedIndexes;
        this.selectedItems = selectedItems;
        if (widgetId == null) {
            throw new ApplicationError("You can't fire this event without a widgetId");
        }
    }
}
