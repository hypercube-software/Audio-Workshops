package com.hypercube.mpm.javafx.widgets.progress;

import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.properties.SceneListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressDialogController extends Controller<ProgressDialog, MainModel> implements SceneListener {

    @FXML
    ProgressBar progressBar;
    @FXML
    Label textProgress;
    @FXML
    Label textHeader;

    @Getter
    private Stage dialogStage;

    @Getter
    private boolean attacedToScene;

    public static ProgressDialogController buildDialog() {
        ProgressDialog progressDialog = new ProgressDialog();
        Stage dialogStage = new Stage();
        dialogStage.setAlwaysOnTop(true);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setTitle("Progression...");
        dialogStage.setScene(new Scene(progressDialog, Color.TRANSPARENT));
        progressDialog.getCtrl().dialogStage = dialogStage;
        return progressDialog.getCtrl();
    }

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

    public void show() {
        if (dialogStage != null) {
            dialogStage.show();
        }
    }

    public void showAndWait() {
        if (dialogStage != null) {
            dialogStage.showAndWait();
        }
    }

    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @Override
    public void onSceneAttach(Scene newValue) {
        attacedToScene = true;
    }

    @Override
    public void onSceneDetach(Scene oldValue) {
        attacedToScene = false;
    }
}
