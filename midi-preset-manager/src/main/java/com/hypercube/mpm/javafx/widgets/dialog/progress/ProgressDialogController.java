package com.hypercube.mpm.javafx.widgets.dialog.progress;

import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.view.properties.SceneListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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

    public void updateProgress(double percent, String msg) {
        runOnJavaFXThread(() -> {
            progressBar.setProgress(percent);
            textProgress.setText(msg);
        });
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
