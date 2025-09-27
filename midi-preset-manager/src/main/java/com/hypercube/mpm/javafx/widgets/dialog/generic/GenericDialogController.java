package com.hypercube.mpm.javafx.widgets.dialog.generic;

import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericDialogController extends DialogController<GenericDialog, MainModel> {

    @FXML
    Label textMessage;

    @FXML
    Label textHeader;

    public void updateText(String header, String msg) {
        runOnJavaFXThread(() -> {
            textHeader.setText(header);
            textMessage.setText(msg);
        });
    }

    @FXML
    public void onCloseButton(ActionEvent event) {
        close();
    }
}
