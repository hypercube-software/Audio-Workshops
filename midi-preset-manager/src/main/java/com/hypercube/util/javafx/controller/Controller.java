package com.hypercube.util.javafx.controller;

import com.hypercube.util.javafx.view.View;
import com.hypercube.util.javafx.view.events.EventHelper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * All controllers will own their widget for convenience
 *
 * @param <T> The widget bound to this controller
 */
@Getter
@Setter
@Slf4j
public abstract class Controller<T extends Node, M> {
    private T view;

    private ObjectProperty<M> model = new SimpleObjectProperty<>();

    public final M getModel() {
        return model.get();
    }

    public final void setModel(M value) {
        model.set(value);
    }

    public ObjectProperty<M> modelProperty() {
        return model;
    }

    public void onPropertyChange(View<?> widget, String property, ObservableValue<? extends String> observable, String oldValue, String newValue) {
        log.info("Property " + widget.getClass()
                .getName() + "::" + property + " changed: " + newValue);
    }

    public <P> P resolvePath(String path) {
        if (path == null) {
            return null;
        }
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(path);
        return (P) expression.getValue(getView());
    }


    public <E extends Event> void addEventListener(Class<E> eventClass, EventHandler<E> callback) {
        getView().addEventHandler(EventHelper.getEventType(eventClass), callback);
    }

    public <E extends Event> void fireEvent(Class<E> eventClass, Object... args) {
        getView().fireEvent(EventHelper.forge(eventClass, getView(), getView(), args));
    }
}
