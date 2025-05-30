package com.hypercube.mpm.javafx.bootstrap;

import com.hypercube.mpm.MidiPresetManagerApplication;
import com.hypercube.mpm.javafx.event.StageReadyEvent;
import com.hypercube.util.javafx.controller.ControllerHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URL;
import java.net.URLDecoder;

/**
 * This class bring Spring and JavaFX together using {@link SpringApplicationBuilder}
 * <p>This class can't be managed by Spring but {@link StageReadyListener} is
 * <p>The application context is passed to the {@link ControllerHelper} that will crete controller beans for all widgets</p>
 */
@Slf4j
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
        //loadFont("fonts/Roboto-Medium.ttf");
        applicationContext.publishEvent(new StageReadyEvent(primaryStage));
    }

    private void loadFont(String fontPath) {
        URL fontUrl = getClass().getClassLoader()
                .getResource(fontPath);
        if (fontUrl != null) {
            String filepath = URLDecoder.decode(fontUrl.toExternalForm());
            Font loadedFont = Font.loadFont(filepath, 10);
            if (loadedFont != null) {
                log.info("Font " + filepath + " loaded, family: " + loadedFont.getFamily());
            } else {
                log.error("Failed to load font from: " + filepath);
            }
        } else {
            log.error("Font not fount:" + fontPath);
        }
    }

    @Override
    public void stop() throws Exception {
        applicationContext.close();
        Platform.exit();
    }

}
