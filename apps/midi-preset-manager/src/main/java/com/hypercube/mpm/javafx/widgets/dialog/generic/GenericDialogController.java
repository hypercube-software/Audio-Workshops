package com.hypercube.mpm.javafx.widgets.dialog.generic;

import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.DialogIcon;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class GenericDialogController extends DialogController<GenericDialog, MainModel> {
    @Setter
    @Getter
    public Runnable onCloseCallback;
    @Getter
    public GenericDialogBehaviour behaviour = GenericDialogBehaviour.NORMAL;
    @Getter
    public boolean cancelled;
    @FXML
    Label textMessage;
    @FXML
    TextField textInput;
    @FXML
    Label textHeader;
    @FXML
    Button cancelButton;
    @FXML
    FontIcon icon;

    public static boolean ask(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.ASK, true);
        dlg.updateText(header, message);
        dlg.setBehaviour(GenericDialogBehaviour.CANCELLABLE);
        dlg.showAndWait();

        return !dlg.cancelled;
    }

    public static void info(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.INFO, true);
        dlg.updateText(header, message);
        dlg.setBehaviour(GenericDialogBehaviour.NORMAL);
        dlg.showAndWait();
    }

    public static void error(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.ERROR, true);
        dlg.updateText(header, message);
        dlg.setBehaviour(GenericDialogBehaviour.NORMAL);
        dlg.showAndWait();
    }

    public static void warn(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.WARNING, true);
        dlg.updateText(header, message);
        dlg.setBehaviour(GenericDialogBehaviour.NORMAL);
        dlg.showAndWait();
    }

    public static Optional<String> input(String header, String message) {
        var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.INFO, true);
        dlg.updateText(header, message);
        dlg.setBehaviour(GenericDialogBehaviour.EDITABLE);
        dlg.showAndWait();

        return Optional.ofNullable(dlg.getView()
                .getTextValue());
    }

    public void setBehaviour(GenericDialogBehaviour behaviour) {
        this.behaviour = behaviour;
        updateVisibleWidgets(behaviour);
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
        cancelled = false;
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        close();
    }

    @FXML
    public void onCancelButton(ActionEvent event) {
        cancelled = true;
        getView().setTextValue(null);
        close();
    }

    private void updateVisibleWidgets(GenericDialogBehaviour behaviour) {
        boolean showInputText = behaviour == GenericDialogBehaviour.EDITABLE;
        textInput.setVisible(showInputText);
        textInput.setManaged(showInputText);
        boolean showCancel = behaviour == GenericDialogBehaviour.EDITABLE || behaviour == GenericDialogBehaviour.CANCELLABLE;
        cancelButton.setVisible(showCancel);
        cancelButton.setManaged(showCancel);
        if (showInputText) {
            textInput.textProperty()
                    .bindBidirectional(getView().textValueProperty());
        }
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
            case ASK:
                icon.setIconLiteral("mdal-help");
                icon.setIconColor(Paint.valueOf("#437fff"));
                break;
            case NONE:
                icon.setIconLiteral("");
                break;
        }
    }
}
