package com.hypercube.util.javafx.controller;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import lombok.Getter;
import lombok.Setter;

/**
 * All controllers will own their widget for convenience
 *
 * @param <T> The widget bound to this controller
 */
@Getter
@Setter
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
}
