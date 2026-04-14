package com.hypercube.util.javafx.controller;

import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialogController;
import com.hypercube.util.javafx.binding.BindingManager;
import com.hypercube.util.javafx.view.View;
import com.hypercube.util.javafx.view.events.EventHelper;
import com.hypercube.util.javafx.worker.LongWork;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

/**
 * All controllers will own their widget for convenience
 *
 * @param <V> The widget bound to this controller
 */
@Getter
@Setter
@Slf4j
public abstract class Controller<V extends Node, M> {
    protected BindingManager bindingManager = new BindingManager(this);
    private ObjectProperty<V> view = new SimpleObjectProperty<>();
    private ObjectProperty<M> model = new SimpleObjectProperty<>();

    //---------------------------------------------------------------
    // observable view
    //---------------------------------------------------------------
    public final V getView() {
        return view.get();
    }

    public final void setView(V value) {
        view.set(value);
    }

    public ObjectProperty<V> viewProperty() {
        return view;
    }

    //---------------------------------------------------------------
    // observable model
    //---------------------------------------------------------------
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
     * Called after {@link FXMLLoader#load} created the view and {@link Initializable#initialize(URL, ResourceBundle)} is done
     */
    public void onViewLoaded() {

    }

    /**
     * Widgets properties are all made available at the same time during creation. So they are all set when this method is called
     * <p>Note: they are NOT available in {@link javafx.fxml.Initializable#initialize}, this is why you have this method</p>
     */
    public void onPropertyChange(View<?> widget, String property, ObservableValue<?> observable, Object oldValue, Object newValue) {
        if (log.isInfoEnabled()) {
            log.info("Property {}::{} changed: {}", widget.getClass()
                    .getName(), property, newValue);
        }
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
            getView().fireEvent(forgeEvent(eventClass, args));
        }
    }

    public <E extends Event> Event forgeEvent(Class<E> eventClass, Object... args) {
        return EventHelper.forge(eventClass, getView(), getView(), args);
    }

    /**
     * Run some code later in the JavaFX thread. Does not wait for its termination
     */
    public void runLaterOnJavaFXThread(Runnable code) {
        Platform.runLater(code);
    }

    /**
     * Run some code in the JavaFX thread and wait for its termination
     */
    public void runOnJavaFXThread(Runnable code) {
        if (Platform.isFxApplicationThread()) {
            code.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    code.run();
                } finally {
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread()
                        .interrupt();
            }
        }
    }

    /**
     * Run a thread to keep the UI thread free displaying a dialog. The thread is named using {@link LongWork#threadName()}
     *
     * @param longWork provide the code to execute in the thread
     */
    public void runLongTaskWithDialog(ProgressDialogController diag, LongWork<Void> longWork) {
        Scene scene = getView().getScene();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread thread = Thread.currentThread();
                String backupName = thread.getName();
                try {
                    thread.setName(longWork.getThreadName() + "-" + thread.threadId());
                    while (!diag.isAttachedToScene()) {
                        sleep(100);
                    }
                    return longWork.getCode()
                            .get();
                } catch (Throwable e) {
                    log.error("Unexpected error", e);
                } finally {
                    thread.setName(backupName);
                }
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            log.info("task {} terminated", longWork.getThreadName());
            Platform.runLater(diag::close);
        });
        task.setOnFailed(event -> {
            log.info("task {} failed", longWork.getThreadName());
            Platform.runLater(diag::close);
        });
        task.setOnCancelled(event -> {
            log.info("task {} cancelled", longWork.getThreadName());
            Platform.runLater(diag::close);
        });
        longWork.setTask(task);
        new Thread(task).start();
        diag.showAndWait();
    }

    /**
     * Run a thread to keep the UI thread free. The thread is named using {@link LongWork#getThreadName()}}
     * <p>Set the cursor to {@link Cursor#WAIT} during the work</p>
     *
     * @param longWork provide the code to execute in the thread
     * @return the task which can be canceled
     */
    public <T> Task<T> runLongTask(LongWork<T> longWork) {
        Scene scene = getView().getScene();
        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                Thread thread = Thread.currentThread();
                String backupName = thread.getName();
                try {
                    thread
                            .setName(longWork.getThreadName() + "-" + thread.threadId());
                    return longWork.getCode()
                            .get();
                } catch (Throwable e) {
                    log.error("Unexpected error", e);
                } finally {
                    thread.setName(backupName);
                }
                return null;
            }
        };
        longWork.setTask(task);
        task.setOnSucceeded(event -> {
            log.warn("Task {} terminated", longWork.getThreadName());
            resetCursor(scene);
        });
        task.setOnFailed(event -> {
            log.warn("Task {} failed", longWork.getThreadName());
            resetCursor(scene);
        });
        task.setOnCancelled(event -> {
            log.warn("Task {} cancelled", longWork.getThreadName());
            resetCursor(scene);
        });
        waitCursor(scene);
        new Thread(task).start();
        return task;
    }

    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetCursor(Scene scene) {
        Platform.runLater(() -> Optional.ofNullable(scene)
                .ifPresent(s -> s.setCursor(Cursor.DEFAULT)));
    }

    private void waitCursor(Scene scene) {
        Optional.ofNullable(scene)
                .ifPresent(s -> s.setCursor(Cursor.WAIT));
    }
}
