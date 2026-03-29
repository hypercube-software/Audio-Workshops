package com.hypercube.mpm.javafx.widgets.menu;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.app.PatchesManager;
import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.javafx.widgets.WidgetIdentifiers;
import com.hypercube.mpm.javafx.widgets.dialog.ports.DevicesPortsDialog;
import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialog;
import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialogController;
import com.hypercube.mpm.javafx.widgets.dialog.sysex.SysexToolboxDialog;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.DialogIcon;
import com.hypercube.util.javafx.controller.JavaFXSpringController;
import com.hypercube.util.javafx.worker.LongWork;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.crawler.CrawlingDomain;
import com.hypercube.workshop.midiworkshop.api.thread.CancelNotifier;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URL;
import java.util.*;

@Slf4j
@JavaFXSpringController
@SuppressWarnings("unused")
public class AppMenuController extends Controller<MenuBar, MainModel> implements Initializable {
    @FXML
    MenuBar root;

    @FXML
    CheckMenuItem menuAlwaysOnTop;
    @Autowired
    PatchesManager patchesManager;
    @Autowired
    DeviceToolBox deviceToolBox;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    DeviceStateManager deviceStateManager;

    @PostConstruct
    public void init() {
        setModel(MainModel.getObservableInstance());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setView(root);
    }

    @FXML
    public void onMenuScanMidiPorts(ActionEvent event) {
        configurationService.getProjectConfiguration()
                .getMidiPortsManager()
                .collectHardwareDevices();
        deviceStateManager.initModel();
    }

    @FXML
    public void onMenuExtractPatchNames(ActionEvent event) {
        if (getModel().getCurrentDeviceState() == null) {
            return;
        }
        var deviceName = getModel().getCurrentDeviceState()
                .getId()
                .getName();
        CrawlingDomain crawlingDomain = new CrawlingDomain(deviceName, Set.of(getModel().getCurrentDeviceState()
                .getId()
                .getMode()),
                new HashSet<>(getModel().getCurrentDeviceState()
                        .getSelectedBankNames())
        );
        CancelNotifier cancelNotifier = new CancelNotifier();
        ProgressDialogController dlg = DialogController.buildDialog(ProgressDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, true);
        dlg.updateTextHeader("Extract patch names from device  '%s'...".formatted(deviceName));
        dlg.updateProgress(0, "");
        dlg.setCancelNotifier(cancelNotifier);
        LongWork longWork = new LongWork("ExtractPatchNames", () -> deviceToolBox.dumpPresets(crawlingDomain, (device, midiPreset, currentCount, totalCount) -> {
            double progress = (double) currentCount / totalCount;
            dlg.updateProgress(progress, "Preset %d/%d: '%s' category '%s'".formatted(currentCount, totalCount, midiPreset.getId()
                    .name(), midiPreset.getId()
                    .category()));
        }, cancelNotifier));
        runLongTaskWithDialog(dlg, longWork);
    }

    @FXML
    public void onMenuOpenDeviceToolBox(ActionEvent event) {
        var dlg = DialogController.buildDialog(SysexToolboxDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, false);
        dlg.showAndWait();
    }

    @FXML
    public void onMenuAllNotesOff(ActionEvent event) {
        ProjectConfiguration projectConfiguration = configurationService.getProjectConfiguration();
        projectConfiguration
                .getMidiDeviceLibrary()
                .getDevices()
                .forEach((name, device) -> projectConfiguration
                        .getMidiPortsManager()
                        .getOutput(device.getOutputMidiDevice())
                        .filter(MidiOutDevice::isOpen)
                        .ifPresent(port -> {
                            log.info("Send all off to MIDI port '{}'...", port.getName());
                            port.sendAllOff();
                        }));
    }

    @FXML
    public void onMenuUpdateCategories(ActionEvent event) {
        runLongTask(new LongWork("updateCategories", () -> {
            getModel().getCurrentDeviceState()
                    .getCurrentSearchOutput()
                    .stream()
                    .filter(patch -> patch.getCategory() == null || patch.getCategory()
                            .equals(MidiPresetCategory.UNKNOWN))
                    .forEach(patch -> getModel().getModeCategories()
                            .stream()
                            .filter(c -> c.matches(patch.getName()))
                            .findFirst()
                            .ifPresent(category -> {
                                log.info("'{}' is category '{}'", patch.getName(), category);
                                patchesManager.changePatchCategory(patch, category.name());
                            }));
            refreshPatches();
        }));
    }

    @FXML
    public void onMenuOpenProject(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(configurationService.getConfigFile()
                .getName());
        fileChooser.setInitialDirectory(configurationService.getConfigFile()
                .getParentFile());
        fileChooser.setTitle("Save project as...");
        fileChooser.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter("MPM Project", "mpm-config.yml")
                );
        fileChooser.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter("All files", "*.*")
                );
        File selectedFile = fileChooser.showOpenDialog(JavaFXApplication.getMainStage());
        if (selectedFile != null) {
            configurationService.setConfigFile(selectedFile);
            configurationService.loadConfig();
        }
    }

    @FXML
    public void onMenuSaveProject(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(configurationService.getConfigFile()
                .getName());
        fileChooser.setInitialDirectory(configurationService.getConfigFile()
                .getParentFile());
        fileChooser.setTitle("Save project as...");
        fileChooser.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter("MPM Project", "mpm-config.yml")
                );
        fileChooser.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter("All files", "*.*")
                );
        File selectedFile = fileChooser.showSaveDialog(JavaFXApplication.getMainStage());
        if (selectedFile != null) {
            configurationService.setConfigFile(selectedFile);
            configurationService.saveConfig();
        }
    }

    @FXML
    public void onMenuExit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void onMenuReloadMidiDeviceLibrary(ActionEvent event) {
        deviceStateManager.reloadMidiDeviceLibrary();
        Optional.ofNullable(getModel().getCurrentDeviceState())
                .flatMap(state -> configurationService.getProjectConfiguration()
                        .getMidiDeviceLibrary()
                        .getDevice(state
                                .getId()
                                .getName()))
                .ifPresent(device -> {
                    getModel().setCurrentDeviceState(null);
                    forceDeviceChange(device.getDeviceName());
                });
        refreshPatches();
    }

    @FXML
    public void onMenuRestoreDeviceState(ActionEvent event) {
        deviceStateManager.initDevices();
    }

    @FXML
    public void onMenuAlwaysOnTop(ActionEvent event) {
        Stage stage = (Stage) getView().getScene()
                .getWindow();
        stage.setAlwaysOnTop(!stage.isAlwaysOnTop());
        menuAlwaysOnTop.setSelected(stage.isAlwaysOnTop());
    }

    @FXML
    public void onMenuDevicesPorts(ActionEvent event) {
        var dlg = DialogController.buildDialog(DevicesPortsDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, false);
        dlg.showAndWait();
    }


    private void refreshPatches() {
        runLaterOnJavaFXThread(() -> {
            patchesManager.refreshPatches();
            deviceStateManager.saveDeviceState();
        });
    }

    private void forceDeviceChange(String deviceName) {
        var selectedIndexes = List.of(getModel().getDevices()
                .indexOf(deviceName));
        List<String> selectedDevice = List.of(deviceName);
        log.info("AppMenuController::forceDeviceChange: {} at index {}", deviceName, selectedIndexes.getFirst());
        fireEvent(SelectionChangedEvent.class, WidgetIdentifiers.WIDGET_ID_DEVICE, selectedIndexes, selectedDevice);
    }
}
