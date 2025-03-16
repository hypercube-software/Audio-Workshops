package com.hypercube.mpm.javafx.bootstrap;

import com.hypercube.mpm.MidiPresetManagerApplication;
import com.hypercube.mpm.javafx.event.StageReadyEvent;
import com.hypercube.util.javafx.controller.ControllerHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This class bring Spring and JavaFX together using {@link SpringApplicationBuilder}
 * <p>This class can't be managed by Spring but {@link StageReadyListener} is
 * <p>The application context is passed to the {@link ControllerHelper} that will crete controller beans for all widgets</p>
 */
public class JavaFXApplication extends Application {
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() throws Exception {
        applicationContext = new SpringApplicationBuilder().sources(MidiPresetManagerApplication.class)
                .build()
                .run();
        ControllerHelper.setSpringContext(applicationContext);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        applicationContext.publishEvent(new StageReadyEvent(primaryStage));
    }

    @Override
    public void stop() throws Exception {
        applicationContext.close();
        Platform.exit();
    }

}
