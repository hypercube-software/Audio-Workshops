package com.hypercube.mpm.javafx.bootstrap;

import com.hypercube.mpm.javafx.event.StageReadyEvent;
import com.hypercube.mpm.javafx.widgets.MainWindow;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * This Spring event listener start JavaFX scene inside a spring context
 * <p>{@link FXMLLoader#setControllerFactory} is used to bring Spring inside JavaFX</p>
 * <p>In this way each controller will be a Spring bean annotated by @Component</p>
 */
@Component
public class StageReadyListener implements ApplicationListener<StageReadyEvent> {

    private final String applicationTitle;
    private final ApplicationContext applicationContext;

    public StageReadyListener(@Value("${spring.application.ui.title}") String applicationTitle, ApplicationContext applicationContext) {
        this.applicationTitle = applicationTitle;
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent stageReadyEvent) {
        Stage stage = stageReadyEvent.getStage();
        MainWindow mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setTitle(this.applicationTitle);
        stage.show();
    }
}
