package com.hypercube.util.javafx.view.properties;

import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handle the lifecycle of a {@link Node} for you and avoid memory leaks related to properties listener
 * <ul>
 *     <li>Detect scene attach and detach</li>
 *     <li>Make sure listeners are released if detached from the scene</li>
 *     <li>Reattach listeners if reattached to the scene</li>
 *     <li>Your controller can implement {@link SceneListener} to be notified of scene attach/detach</li>
 * </ul>
 */
@RequiredArgsConstructor
@Slf4j
public class PropertiesHelper implements SceneListener {
    private record PropertyListener(String propertyName, StringProperty property, ChangeListener<String> listener) {
    }

    private final List<PropertyListener> listeners = new ArrayList<>();
    private final View<?> view;

    /**
     * This method must be called in all views to observe the scene change
     */
    public void installSceneObserver() {
        ((Node) view).sceneProperty()
                .addListener(this::onSceneChange);
    }

    /**
     * This method must be used to observe any property change in a view ({@link StringProperty} only)
     * <p>The controller will be automatically notified via {@link Controller#onPropertyChange}</p>
     *
     * @param propertyName
     * @param property
     */
    public void declareListener(String propertyName, StringProperty property) {
        ChangeListener<String> stringChangeListener = (observableValue, oldValue, newValue) -> view.getController()
                .onPropertyChange(view, propertyName, observableValue, oldValue, newValue);
        listeners.add(new PropertyListener(propertyName, property, stringChangeListener));
    }

    public void onSceneChange(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
        if (oldValue == null && newValue != null) {
            onSceneAttach(newValue);
        } else if (oldValue != null && newValue == null) {
            onSceneDetach(oldValue);
        }
    }

    public void onSceneAttach(Scene newValue) {
        attachPropertyListener();
        if (view.getController() instanceof SceneListener l) {
            l.onSceneAttach(newValue);
        }
    }

    public void onSceneDetach(Scene oldValue) {
        detatchPropertyListener();
        if (view.getController() instanceof SceneListener l) {
            l.onSceneDetach(oldValue);
        }
    }

    private void attachPropertyListener() {
        listeners.forEach(pl -> {
            var listener = pl.listener();
            pl.property()
                    .addListener(listener);
            // force a notification in case we started late
            if (pl.property()
                    .get() != null) {
                listener.changed(pl.property(), null, pl.property()
                        .get());
            }
        });
    }

    private void detatchPropertyListener() {
        listeners.forEach(pl -> pl.property()
                .removeListener(pl.listener()));
    }
}
