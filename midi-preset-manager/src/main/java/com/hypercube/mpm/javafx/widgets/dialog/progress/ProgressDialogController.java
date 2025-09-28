package com.hypercube.mpm.javafx.widgets.dialog.progress;

import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.view.properties.SceneListener;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressDialogController extends DialogController<ProgressDialog, MainModel> implements SceneListener {

    @FXML
    ProgressBar progressBar;
    @FXML
    Label textProgress;
    @FXML
    Label textHeader;

    @Getter
    private boolean attachedToScene;

    public void updateTextHeader(String msg) {
        runOnJavaFXThread(() -> {
            textHeader.setText(msg);
        });
    }

    /**
     * Update the progress without changing the message
     *
     * @param percent in the range [0-1]
     */
    public void updateProgress(double percent) {
        updateProgress(percent, null);
    }

    /**
     * Update the progress
     *
     * @param percent in the range [0-1]
     * @param msg     if not null,  update the message
     */
    public void updateProgress(double percent, String msg) {
        runOnJavaFXThread(() -> {
            animateProgress(percent, 500);
            if (msg != null) {
                textProgress.setText(msg);
            }
        });
    }

    public void animateProgress(double targetProgress, int durationMs) {
        Duration duration = Duration.millis(durationMs);
        KeyValue keyValue = new KeyValue(progressBar.progressProperty(), targetProgress);
        KeyFrame keyFrame = new KeyFrame(duration, keyValue);
        Timeline timeline = new Timeline(keyFrame);
        timeline.play();
    }

    @Override
    public void onSceneAttach(Scene newValue) {
        attachedToScene = true;
    }

    @Override
    public void onSceneDetach(Scene oldValue) {
        attachedToScene = false;
    }
}
