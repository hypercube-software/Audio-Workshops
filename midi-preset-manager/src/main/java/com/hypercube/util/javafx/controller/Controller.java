package com.hypercube.util.javafx.controller;

import com.hypercube.mpm.javafx.widgets.progress.ProgressDialogController;
import com.hypercube.util.javafx.binding.BindingManager;
import com.hypercube.util.javafx.view.View;
import com.hypercube.util.javafx.view.events.EventHelper;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
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
    protected BindingManager bindingManager = new BindingManager(this);
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

    /**
     * Widgets properties are all made available at the same time during creation. So they are all set when this method is called
     * <p>Note: they are NOT available in {@link javafx.fxml.Initializable#initialize}, this is why you have this method</p>
     */
    public void onPropertyChange(View<?> widget, String property, ObservableValue<?> observable, Object oldValue, Object newValue) {
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

    public void runOnJavaFXThread(Runnable code) {
        if (Platform.isFxApplicationThread()) {
            code.run();
        } else {
            Platform.runLater(code);
        }
    }

    public void runLongTaskWithDialog(ProgressDialogController diag, Runnable code) {
        Scene scene = getView().getScene();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    while (!diag.isAttacedToScene()) {
                        sleep(100);
                    }
                    code.run();
                } catch (Throwable e) {
                    log.error("Unexpected error", e);
                    e.printStackTrace();
                }
                return null;
            }
        };
        EventHandler<WorkerStateEvent> closeDialog = e -> {
            Platform.runLater(() -> {
                diag.close();
            });
        };
        task.setOnSucceeded(closeDialog);
        task.setOnFailed(closeDialog);
        task.setOnCancelled(closeDialog);
        new Thread(task).start();
        diag.showAndWait();
    }

    public void runLongTask(Runnable code) {
        Scene scene = getView().getScene();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    code.run();
                } catch (Throwable e) {
                    log.error("Unexpected error", e);
                    e.printStackTrace();
                }
                return null;
            }
        };
        EventHandler<WorkerStateEvent> resetCursor = e -> {
            Platform.runLater(() -> {
                if (scene != null) {
                    scene.setCursor(Cursor.DEFAULT);
                }
            });
        };
        task.setOnSucceeded(resetCursor);
        task.setOnFailed(resetCursor);
        task.setOnCancelled(resetCursor);
        if (scene != null) {
            scene.setCursor(Cursor.WAIT);
        }
        new Thread(task).start();
    }

    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
