package com.hypercube.mpm.javafx.widgets.dialog.generic;

import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.DialogIcon;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class GenericDialogController extends DialogController<GenericDialog, MainModel> {
    @Setter
    @Getter
    public Runnable onCloseCallback;
    @FXML
    Label textMessage;
    @FXML
    Label textHeader;
    @FXML
    FontIcon icon;

    public static void info(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.INFO, true);
        dlg.updateText(header, message);
        dlg.showAndWait();
    }

    public static void error(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.INFO, true);
        dlg.updateText(header, message);
        dlg.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
    }

    public void updateText(String header, String msg) {
        runOnJavaFXThread(() -> {
            textHeader.setText(header);
            textMessage.setText(msg);
        });
    }

    @Override
    public void show() {
        updateIcon();
        super.show();
    }

    @Override
    public void showAndWait() {
        updateIcon();
        super.showAndWait();
    }

    @FXML
    public void onCloseButton(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        close();
    }

    private void updateIcon() {
        switch (dialogIcon) {
            case DialogIcon.WARNING:
                icon.setIconLiteral("mdrmz-warning");
                icon.setIconColor(Paint.valueOf("#ff4d00"));
                break;
            case INFO:
                icon.setIconLiteral("mdal-info");
                icon.setIconColor(Paint.valueOf("#437fff"));
                break;
            case ERROR:
                icon.setIconLiteral("mdal-block");
                icon.setIconColor(Paint.valueOf("#ff0000"));
                break;
            case NONE:
                icon.setIconLiteral("");
                break;
        }
    }
}
