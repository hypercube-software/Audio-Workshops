package com.hypercube.util.javafx.view.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class does the magic to get rid of all {@link EventType} boilerplate imposed by JavaFX Event system
 * <ul>
 *     <li>Internally The {@link EventType} name will be the {@link Event} class name</li>
 *     <li>You get the EventType bound to any Event using {@link EventHelper#getEventType(Class)}</li>
 *     <li>You create an event without bothering about its EvenType using {@link EventHelper#forge}</li>
 * </ul>
 */
@Slf4j
@UtilityClass
public class EventHelper {
    private static Map<Class, EventType<? extends Event>> eventTypes = new HashMap<>();

    public static <E extends Event, ET extends EventType<E>> ET getEventType(Class<E> eventClass) {
        declareEventType(eventClass);
        return (ET) eventTypes.get(eventClass);
    }

    private static <T extends Event> void declareEventType(Class<T> eventClass) {
        if (!eventTypes.containsKey(eventClass)) {
            eventTypes.put(eventClass, new EventType<T>(Event.ANY, eventClass.getSimpleName()));
        }
    }

    public static <T extends Event> T forge(Class<? extends Event> eventClass, Object source, EventTarget target, Object... args) {
        declareEventType(eventClass);
        Object[] constructorParams = new Object[3 + args.length];
        constructorParams[0] = source;
        constructorParams[1] = target;
        constructorParams[2] = eventTypes.get(eventClass);
        for (int i = 0; i < args.length; i++) {
            constructorParams[3 + i] = args[i];
        }
        try {
            var constructor = eventClass.getDeclaredConstructors()[0];
            return (T) constructor.newInstance(constructorParams);
        } catch (InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clone the {@link Event} in case you need to. This seems not usefull since {@link Event#copyFor} do that properly already
     */
    public Event copyFor(Event event, Object newSource, EventTarget newTarget) {
        var fields = Arrays.stream(event.getClass()
                        .getDeclaredFields())
                .map(f -> {
                    try {
                        f.setAccessible(true);
                        return f.get(event);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray();
        return forge(event.getClass(), newSource, newTarget, fields);
    }
}
