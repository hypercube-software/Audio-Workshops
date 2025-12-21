package com.hypercube.util.javafx.controller;

import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.util.javafx.view.View;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ResourceBundle;

public class DialogController<V extends Node, M> extends Controller<V, M> implements Initializable {
    protected DialogIcon dialogIcon;
    @Getter
    protected Stage dialogStage;
    private double xOffset = 0;
    private double yOffset = 0;

    public static <C extends DialogController<?, ?>, ViewClass extends View<C>> C buildDialog(Class<ViewClass> viewClass, Window owner, DialogIcon dialogIcon, boolean modal) {
        try {
            // Creating the view will automatically load the FXML and create the controller for us
            ViewClass dialog = viewClass.getDeclaredConstructor()
                    .newInstance();
            Stage dialogStage = new Stage();
            dialogStage.setAlwaysOnTop(modal);
            dialogStage.initModality(modal ? Modality.APPLICATION_MODAL : Modality.NONE);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setScene(new Scene((Parent) dialog, Color.TRANSPARENT));
            dialogStage.initOwner(owner);
            dialog.getCtrl().dialogStage = dialogStage;
            dialog.getCtrl().dialogIcon = dialogIcon;
            return dialog.getCtrl();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new ApplicationError("Unable to create view class " + viewClass.getName(), e);
        }

    }

    public void setTitle(String title) {
        dialogStage.setTitle(title);
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
    public void initialize(URL url, ResourceBundle resourceBundle) {
        makeDialogDraggable(getView());
    }

    private void makeDialogDraggable(Node node) {
        node.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        node.setOnMouseDragged(event -> {
            dialogStage.setX(event.getScreenX() - xOffset);
            dialogStage.setY(event.getScreenY() - yOffset);
        });
    }
}
