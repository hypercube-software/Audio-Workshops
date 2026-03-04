package com.hypercube.mpm.javafx.event;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

import java.io.File;
import java.util.List;

@Getter
public class FilesDroppedEvent extends Event {
    private final List<File> files;

    public FilesDroppedEvent(Object view, EventTarget target, EventType<? extends Event> eventType, List<File> files) {
        super(view, target, eventType);
        this.files = files;
    }
}
