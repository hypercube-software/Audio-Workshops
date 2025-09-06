package com.hypercube.util.javafx.view.properties;

import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final List<PropertyListener> listeners = new ArrayList<>();
    private final View<?> view;
    private boolean widthSet = false;
    private boolean heightSet = false;

    /**
     * This method must be called in all views to observe the scene change
     */
    public void installSceneObserver() {
        Node node = (Node) view;
        node.sceneProperty()
                .addListener(this::onSceneWindowChange);
    }

    /**
     * This method must be used to observe any property change in a view ({@link StringProperty} only)
     * <p>The controller will be automatically notified via {@link Controller#onPropertyChange}</p>
     *
     * @param propertyName
     * @param property
     */
    public void declareListener(String propertyName, StringProperty property) {
        final Method m = getEventHandler(propertyName, String.class);
        ChangeListener<String> stringChangeListener = (observableValue, oldValue, newValue) -> {
            view.getCtrl()
                    .onPropertyChange(view, propertyName, observableValue, oldValue, newValue);
            try {
                if (m != null) {
                    m.invoke(view.getCtrl(), oldValue, newValue);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
            }
        };
        listeners.add(new PropertyListener(propertyName, property, stringChangeListener));
    }

    /**
     * This method must be used to observe any property change in a view ({@link BooleanProperty} only)
     * <p>The controller will be automatically notified via {@link Controller#onPropertyChange}</p>
     *
     * @param propertyName
     * @param property
     */
    public void declareListener(String propertyName, BooleanProperty property) {
        final Method m = getEventHandler(propertyName, Boolean.class);
        ChangeListener<Boolean> booleanChangeListener = (observableValue, oldValue, newValue) -> {
            view.getCtrl()
                    .onPropertyChange(view, propertyName, observableValue, oldValue, newValue);
            try {
                if (m != null) {
                    m.invoke(view.getCtrl(), oldValue, newValue);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
            }
        };
        listeners.add(new PropertyListener(propertyName, property, booleanChangeListener));
    }

    public void onSceneWindowChange(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
        setMinSizeForStageWindow(newValue);

        if (oldValue != null && newValue == null) {
            onSceneDetach(oldValue);
        }
    }

    public void onSceneAttach(Scene newValue) {
        attachPropertyListener();
        if (view.getCtrl() instanceof SceneListener l) {
            l.onSceneAttach(newValue);
        }
    }

    public void onSceneDetach(Scene oldValue) {
        detachPropertyListener();
        if (view.getCtrl() instanceof SceneListener l) {
            l.onSceneDetach(oldValue);
        }
    }

    /**
     * Set the min size of window is painful. We need to wait "scene" is set, then "scene.window", then "scene.window.width" and "scene.window.height"
     */
    private void setMinSizeForStageWindow(Scene scene) {
        if (scene != null) {
            scene.windowProperty()
                    .addListener((ObservableValue<? extends Window> observableWindowValue, Window oldWindowValue
                            , Window window) -> {
                        window.widthProperty()
                                .addListener((ObservableValue<? extends Number> observableValue, Number oldWidthValue, Number newWidthValue) -> {
                                    if (!widthSet) {
                                        onSceneAttach(scene);
                                        Stage stage = (Stage) window.getScene()
                                                .getWindow();
                                        if (stage.getMinWidth() == 0) {
                                            stage.setMinWidth(newWidthValue.doubleValue());
                                        }
                                        widthSet = true;
                                    }
                                });
                        window.heightProperty()
                                .addListener((ObservableValue<? extends Number> observableValue, Number oldHeighValue, Number newHeightValue) -> {
                                    if (!heightSet) {
                                        Stage stage = (Stage) window.getScene()
                                                .getWindow();
                                        if (stage.getMinHeight() == 0) {
                                            stage.setMinHeight(newHeightValue.doubleValue());
                                        }
                                        heightSet = true;
                                    }
                                });
                    });
        }
    }

    private Method getEventHandler(String propertyName, Class<?> propertyType) {
        Method eventHandlerMethod = null;
        try {
            String methodName = "on" + propertyName.substring(0, 1)
                    .toUpperCase() + propertyName.substring(1) + "Change";
            eventHandlerMethod = view.getCtrl()
                    .getClass()
                    .getMethod(methodName, propertyType, propertyType);
        } catch (NoSuchMethodException e) {
        }
        return eventHandlerMethod;
    }

    private void attachPropertyListener() {
        listeners.forEach(pl -> {
            if (pl.property() instanceof StringProperty sp) {
                ChangeListener<? super String> listener = (ChangeListener<? super String>) pl.listener();
                sp.addListener(listener);
                // force a notification in case we started late
                if (sp.get() != null) {
                    listener.changed(sp, null, sp.get());
                }
            } else if (pl.property() instanceof BooleanProperty bp) {
                ChangeListener<? super Boolean> listener = (ChangeListener<? super Boolean>) pl.listener();
                bp.addListener(listener);
                // force a notification in case we started late
                listener.changed(bp, null, bp.get());
            }
        });
    }

    private void detachPropertyListener() {
        listeners.forEach(pl -> {
            if (pl.property() instanceof StringProperty sp) {
                ChangeListener<? super String> listener = (ChangeListener<? super String>) pl.listener();
                sp.removeListener(listener);
            } else if (pl.property() instanceof BooleanProperty bp) {
                ChangeListener<? super Boolean> listener = (ChangeListener<? super Boolean>) pl.listener();
                bp.removeListener(listener);
            }
        });
    }

    private record PropertyListener(String propertyName, Object property, ChangeListener<?> listener) {
    }
}
