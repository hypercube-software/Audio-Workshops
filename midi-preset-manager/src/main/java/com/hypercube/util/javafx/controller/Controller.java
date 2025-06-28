package com.hypercube.util.javafx.controller;

import com.hypercube.util.javafx.binding.BindingManager;
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
    protected BindingManager bindingManager = new BindingManager(this);

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

    /**
     * Widgets properties are all made available at the same time during creation. So they are all set when this method is called
     * <p>Note: they are NOT available in {@link javafx.fxml.Initializable#initialize}, this is why you have this method</p>
     */
    public void onPropertyChange(View<?> widget, String property, ObservableValue<? extends String> observable, String oldValue, String newValue) {
        log.info("Property " + widget.getClass()
                .getName() + "::" + property + " changed: " + newValue);
    }

    public <P> P resolvePath(String path) {
        return bindingManager.resolvePropertyPath(path);
    }

    public <E extends Event> void addEventListener(Class<E> eventClass, EventHandler<E> callback) {
        if (ControllerHelper.isNonSceneBuilderLaunch()) {
            getView().addEventHandler(EventHelper.getEventType(eventClass), callback);
        }
    }

    public <E extends Event> void fireEvent(Class<E> eventClass, Object... args) {
        if (ControllerHelper.isNonSceneBuilderLaunch()) {
            getView().fireEvent(EventHelper.forge(eventClass, getView(), getView(), args));
        }
    }

}
