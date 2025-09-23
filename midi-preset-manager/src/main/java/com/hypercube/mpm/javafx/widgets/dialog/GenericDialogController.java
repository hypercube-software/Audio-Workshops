package com.hypercube.mpm.javafx.widgets.dialog;

import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.properties.SceneListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericDialogController extends Controller<GenericDialog, MainModel> implements SceneListener {

    @FXML
    Label textMessage;

    @FXML
    Label textHeader;

    @Getter
    private Stage dialogStage;

    @Getter
    private boolean attachedToScene;

    public static GenericDialogController buildDialog() {
        GenericDialog genericDialog = new GenericDialog();
        Stage dialogStage = new Stage();
        dialogStage.setAlwaysOnTop(true);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setTitle("Progression...");
        dialogStage.setScene(new Scene(genericDialog, Color.TRANSPARENT));
        dialogStage.initOwner(JavaFXApplication.getMainStage());
        genericDialog.getCtrl().dialogStage = dialogStage;
        return genericDialog.getCtrl();
    }

    public void updateText(String header, String msg) {
        runOnJavaFXThread(() -> {
            textHeader.setText(header);
            textMessage.setText(msg);
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
        attachedToScene = true;
    }

    @Override
    public void onSceneDetach(Scene oldValue) {
        attachedToScene = false;
    }

    @FXML
    public void onCloseButton(ActionEvent event) {
        close();
    }
}
