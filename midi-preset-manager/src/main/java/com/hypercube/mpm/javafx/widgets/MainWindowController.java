package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.app.PatchesManager;
import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.javafx.event.*;
import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
import com.hypercube.mpm.javafx.widgets.dialog.ports.DevicesPortsDialog;
import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialog;
import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialogController;
import com.hypercube.mpm.javafx.widgets.dialog.sysex.SysexToolboxDialog;
import com.hypercube.mpm.midi.MidiRouter;
import com.hypercube.mpm.midi.VirtualKeyboard;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.DialogIcon;
import com.hypercube.util.javafx.view.properties.SceneListener;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.importer.PatchImporter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sound.midi.MidiUnavailableException;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class MainWindowController extends Controller<MainWindow, MainModel> implements Initializable, SceneListener {
    @Autowired
    MidiRouter midiRouter;
    @Autowired
    VirtualKeyboard virtualKeyboard;
    @Autowired
    DeviceStateManager deviceStateManager;
    @Autowired
    PatchesManager patchesManager;
    @Autowired
    DeviceToolBox deviceToolBox;
    @Autowired
    ConfigurationFactory configurationFactory;
    @FXML
    CheckMenuItem menuAlwaysOnTop;

    @FXML
    public void onMenuScanMidiPorts(ActionEvent event) {
        configurationFactory.getProjectConfiguration()
                .getMidiPortsManager()
                .collectDevices();
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
        ProgressDialogController dlg = DialogController.buildDialog(ProgressDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, true);
        dlg.updateTextHeader("Extract patch names from device  '%s'...".formatted(deviceName));
        dlg.updateProgress(0, "");
        runLongTaskWithDialog(dlg, () -> {
            deviceToolBox.dumpPresets(deviceName, (device, midiPreset, currentCount, totalCount) -> {
                double progress = (double) currentCount / totalCount;
                dlg.updateProgress(progress, "Preset %d/%d: '%s' category '%s'".formatted(currentCount, totalCount, midiPreset.getId()
                        .name(), midiPreset.getId()
                        .category()));
            });
        });
    }

    @FXML
    public void onMenuOpenDeviceToolBox(ActionEvent event) {
        var dlg = DialogController.buildDialog(SysexToolboxDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, false);
        dlg.showAndWait();
    }

    @FXML
    public void onMenuUpdateCategories(ActionEvent event) {
        runLongTask(() -> {
            getModel().getCurrentDeviceState()
                    .getCurrentSearchOutput()
                    .stream()
                    .filter(patch -> patch.getCategory() == null | patch.getCategory()
                            .equals(MidiPresetCategory.UNKNOWN))
                    .forEach(patch -> {
                        getModel().getModeCategories()
                                .stream()
                                .filter(c -> c.matches(patch.getName()))
                                .findFirst()
                                .ifPresent(category -> {
                                    log.info("'{}' is category '{}'", patch.getName(), category);
                                    patchesManager.changePatchCategory(patch, category.name());
                                });
                    });
            refreshPatches();
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());
        deviceStateManager.initModel();
        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
        addEventListener(FilesDroppedEvent.class, this::onFilesDropped);
        addEventListener(MuteOutputDeviceEvent.class, this::onMuteOutputDevice);
    }

    @Override
    public void onSceneAttach(Scene newScene) {
        try {
            initDevices();
        } catch (MidiError e) {
            showError(e);
        }
        try {
            newScene.setOnKeyPressed(virtualKeyboard::translateKeyDown);
            newScene.setOnKeyReleased(virtualKeyboard::translateKeyUp);
            restoreConfigSelection();
        } catch (MidiError e) {
            showError(e);
        }
    }

    @Override
    public void onSceneDetach(Scene oldValue) {

    }

    @FXML
    public void onMenuOpenProject(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(configurationFactory.getConfigFile()
                .getName());
        fileChooser.setInitialDirectory(configurationFactory.getConfigFile()
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
            configurationFactory.setConfigFile(selectedFile);
            configurationFactory.loadConfig();
        }
    }

    @FXML
    public void onMenuSaveProject(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(configurationFactory.getConfigFile()
                .getName());
        fileChooser.setInitialDirectory(configurationFactory.getConfigFile()
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
            configurationFactory.setConfigFile(selectedFile);
            configurationFactory.saveConfig();
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
                .flatMap(state -> configurationFactory.getProjectConfiguration()
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
        initDevices();
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

    @FXML
    public void onAllNotesOff(ActionEvent event) {
        ProjectConfiguration projectConfiguration = configurationFactory.getProjectConfiguration();
        projectConfiguration
                .getMidiDeviceLibrary()
                .getDevices()
                .forEach((name, device) -> {
                    projectConfiguration
                            .getMidiPortsManager()
                            .getOutput(device.getOutputMidiDevice())
                            .filter(MidiOutDevice::isOpen)
                            .ifPresent(port -> {
                                log.info("Send all off to MIDI port '{}'...", port.getName());
                                port.sendAllOff();
                            });
                });
    }

    private void showError(Throwable error) {
        String errorClassName = error.getClass()
                .getSimpleName();
        String deviceName = null;
        if (error instanceof MidiError midiError) {
            deviceName = midiError.getDeviceName(); // can be null
        }
        String msg = getMessageFromError(error);
        String title = "Unexpected error" + Optional.ofNullable(deviceName)
                .map(name -> " on device '" + name + "'")
                .orElse("");

        runOnJavaFXThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.getDialogPane()
                    .getStylesheets()
                    .add(MainWindowController.class.getResource("MainWindow.css")
                            .toExternalForm());
            alert.getDialogPane()
                    .getStyleClass()
                    .add("dialog-pane");
            alert.setHeaderText("A %s occured".formatted(errorClassName));
            alert.setTitle(title);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private String getMessageFromError(Throwable error) {
        String msg = error.getMessage();
        while (error.getCause() != null) {
            if (error.getCause() instanceof MidiUnavailableException) {
                msg = "The MIDI device is already taken by another application";
                break;
            } else {
                msg = error.getCause()
                        .getMessage();
            }
            error = error.getCause();
        }
        return msg;
    }

    /**
     * Restore the state of all devices when the application start
     */
    private void initDevices() {
        var cfg = configurationFactory.getProjectConfiguration();
        List<String> devices = getModel().getDevices();
        log.info("Midi Device Library active: {}", devices.size());
        if (devices
                .isEmpty()) {
            GenericDialogController.info("First Launch", """
                    This is the first time you run this application.
                    There is no device enabled yet in your library.
                    You need to assign MIDI Ports to devices you want to use.
                    Then they will appear in the list.
                    """);
            onMenuDevicesPorts(null);
            onMenuReloadMidiDeviceLibrary(null);
        } else if (!cfg.getSelectedPatches()
                .isEmpty()) {
            ProgressDialogController dlg = DialogController.buildDialog(ProgressDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, true);
            dlg.updateTextHeader("Restore %d device states...".formatted(cfg.getSelectedPatches()
                    .size()));
            runLongTaskWithDialog(dlg, () -> {
                var sp = cfg.getSelectedPatches();
                int totalSteps = sp.size() * 2 + 1;
                dlg.updateProgress(0, "Wake up MIDI out devices with ActiveSensing...");
                AtomicInteger stepsCount = new AtomicInteger();
                IntStream.range(0, sp.size())
                        .parallel()
                        .forEach(i -> {
                            var device = cfg.getMidiDeviceLibrary()
                                    .getDevice(sp.get(i)
                                            .getDevice())
                                    .orElseThrow();
                            cfg.getMidiPortsManager()
                                    .getOutput(device.getOutputMidiDevice())
                                    .ifPresent(out -> {
                                        log.info("Wake up device '{}' on MIDI port '{}'", device.getDeviceName(), out.getName());
                                        try {
                                            out.open();
                                            out.sleep(4000);
                                            out.close();
                                        } catch (MidiError e) {
                                            log.error("Unexpected error wakening device {}", device.getDeviceName(), e);
                                        }
                                    });
                            double currentTotal = (double) stepsCount.incrementAndGet() / totalSteps;
                            dlg.updateProgress(currentTotal);
                        });
                for (int i = 0; i < sp.size(); i++) {
                    var selectedPatch = sp.get(i);
                    double progress = (double) stepsCount.incrementAndGet() / totalSteps;
                    dlg.updateProgress(progress, "'%s' on '%s' ...".formatted(selectedPatch.getName(), selectedPatch.getDevice()));
                    deviceStateManager.initDeviceStateWithPatch(selectedPatch);
                    patchesManager.sendPatchToDevice(selectedPatch);
                    MidiOutDevice port = getModel().getCurrentDeviceState()
                            .getMidiOutDevice();
                    if (port != null) {
                        try {
                            port.close();
                        } catch (MidiError e) {
                            log.error("Unexpected error closing MIDI port {}", port.getName(), e);
                        }
                    }
                }
                onDeviceChanged(null);
                midiRouter.setControllerMessageListener(this::onMidiController);
                midiRouter.listenDawOutputs();
                Optional.ofNullable(cfg.getSelectedOutput())
                        .ifPresent(this::forceDeviceChange);

                dlg.updateProgress(1, "Done");
                sleep(3000);
            });
        }
    }

    private void forceDeviceChange(String deviceName) {
        fireEvent(SelectionChangedEvent.class, WidgetIdentifiers.WIDGET_ID_DEVICE, List.of(), List.of(deviceName));
    }

    private void onPatchScoreChanged(PatchScoreChangedEvent patchScoreChangedEvent) {
        patchesManager.onPatchScoreChanged(patchScoreChangedEvent.getPatch());
    }

    /**
     * Method called when {@link com.hypercube.mpm.midi.MidiTransformer} see a controller message
     *
     * @param message to be displayed to the user
     */
    private void onMidiController(String message) {
        runOnJavaFXThread(() -> getModel().setEventInfo(message));
    }


    private void onFilesDropped(FilesDroppedEvent filesDroppedEvent) {
        try {
            var cfg = configurationFactory.getProjectConfiguration();

            MainModel model = getModel();
            MidiDeviceLibrary midiDeviceLibrary = cfg.getMidiDeviceLibrary();
            PatchImporter patchImporter = new PatchImporter(midiDeviceLibrary);
            var state = model.getCurrentDeviceState();
            if (state != null) {
                var device = midiDeviceLibrary
                        .getDevice(model
                                .getCurrentDeviceState()
                                .getId()
                                .getName())
                        .orElseThrow();
                filesDroppedEvent.getFiles()
                        .forEach(f -> patchImporter.importSysex(device, state.getId()
                                .getMode(), f));

                midiDeviceLibrary
                        .collectCustomBanksAndPatches(device);
                deviceStateManager.refreshModeProperties(device, state);
            }
        } catch (Exception e) {
            log.error("Unexpected error:", e);
        }
    }

    private void onSearchPatches(SearchPatchesEvent searchPatchesEvent) {
        refreshPatches();
    }

    private void onSelectionChanged(SelectionChangedEvent<?> selectionChangedEvent) {
        String widgetId = selectionChangedEvent.getWidgetId();
        log.info("{} changed ! Indexes: {}", widgetId, selectionChangedEvent.getSelectedIndexes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        switch (widgetId) {
            case WidgetIdentifiers.WIDGET_ID_DEVICE ->
                    runLongTask(() -> onDeviceChanged((SelectionChangedEvent<String>) selectionChangedEvent));
            case WidgetIdentifiers.WIDGET_ID_MODE ->
                    runLongTask(() -> onModeChanged((SelectionChangedEvent<String>) selectionChangedEvent));
            case WidgetIdentifiers.WIDGET_ID_MODE_CHANNEL ->
                    onChannelChanged((SelectionChangedEvent<Integer>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_CATEGORY ->
                    onCategoriesChanged((SelectionChangedEvent<MidiPresetCategory>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_BANK ->
                    onBanksChanged((SelectionChangedEvent<String>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PATCH ->
                    onPatchChanged((SelectionChangedEvent<Patch>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PASSTHRU_OUTPUTS ->
                    runLongTask(() -> onSecondaryOutputsChanged((SelectionChangedEvent<String>) selectionChangedEvent));
            case WidgetIdentifiers.WIDGET_ID_MASTER_INPUTS ->
                    runLongTask(() -> onMasterInputsChanged((SelectionChangedEvent<String>) selectionChangedEvent));
        }
    }

    private void onChannelChanged(SelectionChangedEvent<Integer> selectionChangedEvent) {
        selectionChangedEvent.getSelectedItems()
                .stream()
                .findFirst()
                .ifPresent(this::changeOuputChannel);
    }

    private void changeOuputChannel(int channel) {
        log.info("Change output channel: {}", channel);
        var model = getModel();
        deviceStateManager.onChannelChanged(model, channel);
        midiRouter.changeOutputChannel(channel);
        refreshPatches();
    }

    private void restoreConfigSelection() {
        var cfg = configurationFactory.getProjectConfiguration();
        Optional.ofNullable(cfg.getSelectedOutput())
                .ifPresent(this::forceDeviceChange);
        Optional.ofNullable(cfg.getSelectedInputs())
                .ifPresent(selectedInputs -> {
                    List<String> inputPorts = getModel().getMidiInPorts()
                            .stream()
                            .filter(selectedInputs::contains)
                            .toList();
                    getModel().setSelectedInputPorts(inputPorts);
                    midiRouter.changeMasterInputs(inputPorts);
                });
        Optional.ofNullable(cfg.getSelectedSecondaryOutputs())
                .ifPresent(selectedSecondaryOutputPorts -> {
                    getModel().setSelectedOutputPorts(selectedSecondaryOutputPorts);
                    midiRouter.changeSecondaryOutputs(selectedSecondaryOutputPorts);
                });
    }

    private void onMuteOutputDevice(MuteOutputDeviceEvent muteOutputDeviceEvent) {
        midiRouter.mute(muteOutputDeviceEvent.getDevice(), muteOutputDeviceEvent.isMute());
    }

    private void onMasterInputsChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        var cfg = configurationFactory.getProjectConfiguration();

        List<String> deviceOrPortNames = selectionChangedEvent.getSelectedItems();
        try {
            midiRouter.changeMasterInputs(deviceOrPortNames);
            cfg.setSelectedInputs(deviceOrPortNames);
            configurationFactory.saveConfig();
        } catch (MidiError e) {
            showError(e);
        }
    }

    private void onSecondaryOutputsChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        var cfg = configurationFactory.getProjectConfiguration();

        List<String> selectedItems = selectionChangedEvent.getSelectedItems();
        midiRouter.changeSecondaryOutputs(selectedItems);
        cfg.setSelectedSecondaryOutputs(selectedItems);
        configurationFactory.saveConfig();
    }

    private void onCategoriesChanged(SelectionChangedEvent<MidiPresetCategory> selectionChangedEvent) {
        var model = getModel();
        var state = model.getCurrentDeviceState();
        if (state != null) {
            state.setCurrentSelectedCategories(selectionChangedEvent.getSelectedItems());
        }
        refreshPatches();
    }

    private void onBanksChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        var model = getModel();
        List<String> selectedItems = selectionChangedEvent.getSelectedItems();
        if (!selectedItems.isEmpty()) {
            model.getCurrentDeviceState()
                    .setSelectedBankNames(selectedItems);
        } else {
            model.getCurrentDeviceState()
                    .setSelectedBankNames(List.of());
        }
        refreshPatches();
    }

    private void onPatchChanged(SelectionChangedEvent<Patch> selectionChangedEvent) {
        var model = getModel();
        List<Patch> selectedItems = selectionChangedEvent.getSelectedItems();
        if (!selectedItems.isEmpty()) {
            Patch patch = selectedItems
                    .getFirst();
            if (!patch.equals(model.getCurrentDeviceState()
                    .getCurrentPatch())) {
                patchesManager.setCurrentPatch(patch);
                deviceStateManager.saveDeviceState();
            }
        } else {
            patchesManager.setCurrentPatch(null);
            deviceStateManager.saveDeviceState();
        }
    }

    /**
     * Called when the user change device mode
     */
    private void onModeChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        List<String> selectedItems = selectionChangedEvent.getSelectedItems();
        if (selectedItems.isEmpty()) {
            var model = getModel();
            log.info("No mode selected, emptying everything...");
            model.setModeCategories(List.of());
            model.setModeChannels(List.of());
            model.setModeBanks(List.of());
            model.getCurrentDeviceState()
                    .setCurrentSearchOutput(List.of());
        } else {
            deviceStateManager.onModeChanged(selectedItems);
            midiRouter.changeOutputChannel(getModel().getCurrentDeviceState()
                    .getId()
                    .getChannel());
            refreshPatches();
        }
    }

    /**
     * Update the view given a selected device and select this device as the main destination for the MIDI router
     */
    private void onDeviceChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        var cfg = configurationFactory.getProjectConfiguration();

        MidiDeviceDefinition device = Optional.ofNullable(selectionChangedEvent)
                .map(evt -> selectionChangedEvent.getSelectedItems())
                .filter(selectedItems -> selectedItems.size() == 1)
                .map(selectedItems -> {
                    String deviceName = selectedItems
                            .getFirst();
                    getModel().setSelectedDevice(deviceName);
                    cfg.setSelectedOutput(deviceName);
                    configurationFactory.saveConfig();
                    return cfg.getMidiDeviceLibrary()
                            .getDevice(deviceName)
                            .orElseThrow();
                })
                .orElse(null);
        if (getModel().getCurrentDeviceState() == null || getModel().getCurrentDeviceState()
                .getMidiOutDevice() == null || device == null || !getModel().getCurrentDeviceState()
                .getId()
                .getName()
                .equals(device.getDeviceName())) {
            deviceStateManager.onDeviceChanged(device);
            try {
                midiRouter.changeMainDestination(device);
            } catch (MidiError e) {
                log.error("Unexpected error in Midi Router", e);
            }
            refreshPatches();
        }
    }

    private void refreshPatches() {
        patchesManager.refreshPatches();
        deviceStateManager.saveDeviceState();
    }
}
