package com.hypercube.mpm.javafx.widgets.dialog.project;

import com.hypercube.mpm.javafx.widgets.button.IconButton;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.JavaFXSpringController;
import com.hypercube.util.javafx.model.ModelHelper;
import com.hypercube.util.javafx.view.lists.ListHelper;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@JavaFXSpringController
public class ProjectDialogController extends DialogController<ProjectDialog, ProjectDialogViewModel> {
    @FXML
    TableColumn<DeviceState, Boolean> colLastUsed;
    @FXML
    TableColumn<DeviceState, String> colDevice;
    @FXML
    TableColumn<DeviceState, String> colMode;
    @FXML
    TableColumn<DeviceState, String> colChannel;
    @FXML
    TableColumn<DeviceState, String> colPatch;
    @FXML
    TableView<DeviceState> statesList;

    @Override
    @SuppressWarnings("unchecked")
    public void onViewLoaded() {
        setModel(ModelHelper.forgeMMVM(new ProjectDialogViewModel()));

        var toEdit = MainModel.getObservableInstance()
                .getDeviceStates()
                .values()
                .stream()
                .sorted(Comparator.comparing(DeviceState::getId))
                .toList();
        getModel().setDeviceStates(toEdit);
        ListHelper.configureColumn(colLastUsed, "lastUsed", getWidgetFactory(), this::onIconButtonUpdate);
        ListHelper.configureColumn(colDevice, "id.name");
        ListHelper.configureColumn(colMode, "id.mode");
        ListHelper.configureColumn(colChannel, "id.channel");
        ListHelper.configureColumn(colPatch, "currentPatch.name");
        ListHelper.allowMultiSelection(statesList);
        statesList.setItems((ObservableList<DeviceState>) getModel().getDeviceStates());
    }

    @FXML
    public void onCancelButton(ActionEvent event) {
        close();
    }

    @FXML
    public void onSaveButton(ActionEvent event) {
        close();
    }

    private Supplier<IconButton> getWidgetFactory() {
        return () -> {
            IconButton b = new IconButton();
            b.getStyleClass()
                    .setAll("small-icon-button");
            return b;
        };
    }

    private Void onIconButtonUpdate(IconButton iconButton, Boolean lastUsed) {
        if (Optional.ofNullable(lastUsed)
                .map(Boolean::booleanValue)
                .orElse(false)) {
            iconButton.setIconClass("icon-caret-right,tiny");
        } else {
            iconButton.setIconClass("icon-null");
        }
        return null;
    }
}
