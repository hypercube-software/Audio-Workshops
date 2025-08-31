package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.mpm.app.PatchesManager;
import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
import com.hypercube.mpm.javafx.event.PatchScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.javafx.widgets.progress.ProgressDialogController;
import com.hypercube.mpm.midi.MidiRouter;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
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
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sound.midi.MidiUnavailableException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainWindowController extends Controller<MainWindow, MainModel> implements Initializable, SceneListener {
    @Autowired
    ProjectConfiguration cfg;
    @Autowired
    MidiRouter midiRouter;
    @Autowired
    DeviceStateManager deviceStateManager;
    @Autowired
    PatchesManager patchesManager;
    @Autowired
    ConfigurationFactory configurationFactory;
    @FXML
    CheckMenuItem menuAlwaysOnTop;

    public void onUpdateCategories(ActionEvent event) {
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

        try {
            initDevices();
        } catch (MidiError e) {
            showError(e);
        }
    }

    @Override
    public void onSceneAttach(Scene newValue) {
        try {
            restoreConfigSelection();
        } catch (MidiError e) {
            showError(e);
        }
    }

    @Override
    public void onSceneDetach(Scene oldValue) {

    }

    @FXML
    public void onMenuExit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void onMenuReloadMidiDeviceLibrary(ActionEvent event) {
        deviceStateManager.reloadMidiDeviceLibrary();
        Optional.ofNullable(getModel().getCurrentDeviceState())
                .flatMap(state -> cfg.getMidiDeviceLibrary()
                        .getDevice(state
                                .getId()
                                .getName()))
                .ifPresent(device -> {
                    getModel().setCurrentDeviceState(null);
                    forceDeviceChange(device.getDeviceName());
                });
        refreshPatches();
    }

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
    public void onAllNotesOff(ActionEvent event) {
        cfg.getMidiDeviceLibrary()
                .getDevices()
                .forEach((name, device) -> {
                    cfg.getMidiDeviceManager()
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
        if (!cfg.getSelectedPatches()
                .isEmpty()) {
            var dlg = ProgressDialogController.buildDialog();
            dlg.updateTextHeader("Restore %d device states...".formatted(cfg.getSelectedPatches()
                    .size()));
            runLongTaskWithDialog(dlg, () -> {
                var sp = cfg.getSelectedPatches();
                for (int i = 0; i < sp.size(); i++) {
                    var selectedPatch = sp.get(i);
                    double progress = (double) i / sp.size();
                    dlg.updateProgress(progress, "'%s' on '%s' ...".formatted(selectedPatch.getName(), selectedPatch.getDevice()));
                    deviceStateManager.initDeviceStateWithPatch(selectedPatch);
                    patchesManager.sendPatchToDevice(selectedPatch);
                }
                onDeviceChanged(null);
                midiRouter.setControllerMessageListener(this::onMidiController);
                midiRouter.listenDawOutputs();
                Optional.ofNullable(cfg.getSelectedOutput())
                        .ifPresent(this::forceDeviceChange);

                dlg.updateProgress(1, "Done");
                sleep(4000);
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
                        .collectCustomPatches(device);
                deviceStateManager.refreshModeProperties(device);
            }
        } catch (Exception e) {
            log.error("Unexpected error:", e);
        }
    }

    private void onSearchPatches(SearchPatchesEvent searchPatchesEvent) {
        refreshPatches();
    }

    private void onSelectionChanged(SelectionChangedEvent selectionChangedEvent) {
        String widgetId = selectionChangedEvent.getWidgetId();
        log.info("{} changed ! {}", widgetId, selectionChangedEvent.getSelectedIndexes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        switch (widgetId) {
            case WidgetIdentifiers.WIDGET_ID_DEVICE -> runLongTask(() -> onDeviceChanged(selectionChangedEvent));
            case WidgetIdentifiers.WIDGET_ID_MODE -> runLongTask(() -> onModeChanged(selectionChangedEvent));
            case WidgetIdentifiers.WIDGET_ID_MODE_CHANNEL -> onChannelChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_CATEGORY -> onCategoriesChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_BANK -> onBankChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PATCH -> onPatchChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PASSTHRU_OUTPUTS ->
                    runLongTask(() -> onSecondaryOutputsChanged(selectionChangedEvent));
            case WidgetIdentifiers.WIDGET_ID_MASTER_INPUTS ->
                    runLongTask(() -> onMasterInputsChanged(selectionChangedEvent));
        }
    }

    private void onChannelChanged(SelectionChangedEvent<Integer> selectionChangedEvent) {
        var model = getModel();
        selectionChangedEvent.getSelectedItems()
                .stream()
                .findFirst()
                .ifPresent(channel -> {
                    deviceStateManager.onChannelChanged(model, channel);
                    midiRouter.changeOutputChannel(channel);
                });
        refreshPatches();
    }

    private void restoreConfigSelection() {
        Optional.ofNullable(cfg.getSelectedOutput())
                .ifPresent(deviceName -> {
                    getModel().setSelectedDevice(deviceName);
                });
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

    private void onMasterInputsChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        List<String> deviceOrPortNames = selectionChangedEvent.getSelectedItems();
        try {
            midiRouter.changeMasterInputs(deviceOrPortNames);
            cfg.setSelectedInputs(deviceOrPortNames);
            configurationFactory.saveConfig(cfg);
        } catch (MidiError e) {
            showError(e);
        }
    }

    private void onSecondaryOutputsChanged(SelectionChangedEvent selectionChangedEvent) {
        List<String> selectedItems = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(String.class::cast)
                .toList();
        midiRouter.changeSecondaryOutputs(selectedItems);
        cfg.setSelectedSecondaryOutputs(selectedItems);
        configurationFactory.saveConfig(cfg);
    }

    private void onCategoriesChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        List<MidiPresetCategory> selectedItems = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(MidiPresetCategory.class::cast)
                .toList();
        var state = model.getCurrentDeviceState();
        if (state != null) {
            state.setCurrentSelectedCategories(selectedItems);
        }
        refreshPatches();
    }

    private void onBankChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            String bankName = (String) selectionChangedEvent.getSelectedItems()
                    .getFirst();
            model.getCurrentDeviceState()
                    .setCurrentBank(bankName);
        } else {
            model.getCurrentDeviceState()
                    .setCurrentBank(null);
        }
        refreshPatches();
    }

    private void onPatchChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            Patch patch = (Patch) selectionChangedEvent.getSelectedItems()
                    .getFirst();
            if (!patch.equals(model.getCurrentDeviceState()
                    .getCurrentPatch())) {
                patchesManager.setCurrentPatch(patch);
                deviceStateManager.saveDeviceState();
            }
        }
    }

    /**
     * Called when the user change device mode
     */
    private void onModeChanged(SelectionChangedEvent selectionChangedEvent) {
        if (selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            var model = getModel();
            log.info("No mode selected, emptying everything...");
            model.setModeCategories(List.of());
            model.setModeChannels(List.of());
            model.setModeBanks(List.of());
            model.getCurrentDeviceState()
                    .setCurrentSearchOutput(List.of());
        } else {
            deviceStateManager.onModeChanged(selectionChangedEvent.getSelectedItems());
            refreshPatches();
        }
    }

    /**
     * Update the view given a selected device and select this device as the main destination for the MIDI router
     */
    private void onDeviceChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        MidiDeviceDefinition device = Optional.ofNullable(selectionChangedEvent)
                .map(evt -> selectionChangedEvent.getSelectedItems())
                .filter(selectedItems -> selectedItems.size() == 1)
                .map(selectedItems -> {
                    String deviceName = selectedItems
                            .getFirst();
                    getModel().setSelectedDevice(deviceName);
                    cfg.setSelectedOutput(deviceName);
                    configurationFactory.saveConfig(cfg);
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
            midiRouter.changeMainDestination(device);
            refreshPatches();
        }
    }

    private void refreshPatches() {
        patchesManager.refreshPatches();
        deviceStateManager.saveDeviceState();
    }
}
