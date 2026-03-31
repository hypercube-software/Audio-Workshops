package com.hypercube.mpm.javafx.widgets.dialog.project;

import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.mpm.javafx.widgets.button.IconButton;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.JavaFXSpringController;
import com.hypercube.util.javafx.view.lists.ListHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@JavaFXSpringController
public class ProjectDialogController extends DialogController<ProjectDialog, Void> {
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

    @Autowired
    private ConfigurationService configurationService;

    private ObservableList<DeviceState> editedStates;
    private List<DeviceState> removedStates = new ArrayList<>();

    @Override
    public void onViewLoaded() {

        editedStates = FXCollections.observableList(MainModel.getObservableInstance()
                .getDeviceStates()
                .values()
                .stream()
                .filter(s -> s.getCurrentPatch() != null)
                .sorted(Comparator.comparing(DeviceState::getId))
                .collect(Collectors.toList()));

        ListHelper.configureColumn(colLastUsed, "lastUsed", getWidgetFactory(), this::onIconButtonUpdate);
        ListHelper.configureColumn(colDevice, "id.name");
        ListHelper.configureColumn(colMode, "id.mode");
        ListHelper.configureColumn(colChannel, "id.channel");
        ListHelper.configureColumn(colPatch, "currentPatch.name");
        ListHelper.allowMultiSelection(statesList);
        statesList.setItems((ObservableList<DeviceState>) editedStates);
    }

    @FXML
    public void onCancelButton(ActionEvent event) {
        close();
    }

    @FXML
    public void onSaveButton(ActionEvent event) {
        configurationService.getProjectConfiguration()
                .removeSelectedPatches(removedStates);
        configurationService.saveConfig();
        MainModel mainModel = MainModel.getObservableInstance();
        removedStates.forEach(state -> {
            mainModel.getDeviceStates()
                    .get(state.getId())
                    .setCurrentPatch(null);
        });
        close();
    }

    @FXML
    public void onDeleteStates(ActionEvent event) {
        List<DeviceState> selected = ListHelper.getSelectedItems(statesList);
        editedStates.removeAll(selected);
        removedStates.addAll(selected);
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
