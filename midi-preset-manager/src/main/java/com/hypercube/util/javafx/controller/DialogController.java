package com.hypercube.util.javafx.controller;

import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.util.javafx.view.View;
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

public class DialogController<V extends Node, M> extends Controller<V, M> {
    @Getter
    Stage dialogStage;

    public static <C extends DialogController<?, ?>, ViewClass extends View<C>> C buildDialog(Class<ViewClass> viewClass, Window owner, boolean modal) {
        try {
            // Creating the view will automatically load the FXML and create the controller for us
            ViewClass dialog = viewClass.getDeclaredConstructor()
                    .newInstance();
            Stage dialogStage = new Stage();
            dialogStage.setAlwaysOnTop(true);
            dialogStage.initModality(modal ? Modality.APPLICATION_MODAL : Modality.NONE);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setScene(new Scene((Parent) dialog, Color.TRANSPARENT));
            dialogStage.initOwner(owner);
            dialog.getCtrl().dialogStage = dialogStage;
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
}
