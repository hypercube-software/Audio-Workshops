package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.app.PatchesManager;
import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.mpm.javafx.event.*;
import com.hypercube.mpm.midi.MidiRouter;
import com.hypercube.mpm.midi.VirtualKeyboard;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.controller.JavaFXSpringController;
import com.hypercube.util.javafx.view.properties.SceneListener;
import com.hypercube.util.javafx.worker.LongWork;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.importer.PatchImporter;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sound.midi.MidiUnavailableException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@JavaFXSpringController
@SuppressWarnings("unused")
public class MainWindowController extends Controller<MainWindow, MainModel> implements SceneListener {
    @Autowired
    MidiRouter midiRouter;
    @Autowired
    PatchImporter patchImporter;
    @Autowired
    VirtualKeyboard virtualKeyboard;
    @Autowired
    DeviceStateManager deviceStateManager;
    @Autowired
    PatchesManager patchesManager;
    @Autowired
    DeviceToolBox deviceToolBox;
    @Autowired
    ConfigurationService configurationService;

    @Override
    public void onViewLoaded() {
        setModel(MainModel.getObservableInstance());

        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
        addEventListener(FilesDroppedEvent.class, this::onFilesDropped);
        addEventListener(MuteOutputDeviceEvent.class, this::onMuteOutputDevice);
    }

    @Override
    public void onSceneAttach(Scene newScene) {
        try {
            deviceStateManager.initDevices();
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


    private void forceDeviceChange(String deviceName) {
        var selectedIndexes = List.of(getModel().getDevices()
                .indexOf(deviceName));
        List<String> selectedDevice = List.of(deviceName);
        log.info("MainWindowController::forceDeviceChange: {} at index {}", deviceName, selectedIndexes.getFirst());
        fireEvent(SelectionChangedEvent.class, WidgetIdentifiers.WIDGET_ID_DEVICE, selectedIndexes, selectedDevice);
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
            var cfg = configurationService.getProjectConfiguration();

            MainModel model = getModel();
            MidiDeviceLibrary midiDeviceLibrary = cfg.getMidiDeviceLibrary();
            var state = model.getCurrentDeviceState();
            if (state != null) {
                var device = midiDeviceLibrary
                        .getDevice(model
                                .getCurrentDeviceState()
                                .getId()
                                .getName())
                        .orElseThrow();
                filesDroppedEvent.getFiles()
                        .forEach(f -> patchImporter.importSysExFile(device, state.getId()
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

    @SuppressWarnings("unchecked")
    private void onSelectionChanged(SelectionChangedEvent<?> selectionChangedEvent) {
        selectionChangedEvent.consume();
        String widgetId = selectionChangedEvent.getWidgetId();
        log.info("{} {} changed ! Selected indexes: [{}]", this.toString(), widgetId, selectionChangedEvent.getSelectedIndexes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        switch (widgetId) {
            case WidgetIdentifiers.WIDGET_ID_DEVICE ->
                    runLongTask(new LongWork<Void>("selectedDeviceChanged", () -> onDeviceChanged((SelectionChangedEvent<String>) selectionChangedEvent)));
            case WidgetIdentifiers.WIDGET_ID_MODE ->
                    runLongTask(new LongWork<Void>("selectedModeChanged", () -> onModeChanged((SelectionChangedEvent<String>) selectionChangedEvent)));
            case WidgetIdentifiers.WIDGET_ID_MODE_CHANNEL ->
                    onChannelChanged((SelectionChangedEvent<Integer>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_CATEGORY ->
                    onCategoriesChanged((SelectionChangedEvent<MidiPresetCategory>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_BANK ->
                    onBanksChanged((SelectionChangedEvent<String>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PATCH ->
                    onPatchChanged((SelectionChangedEvent<Patch>) selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PASSTHRU_OUTPUTS ->
                    runLongTask(new LongWork<Void>("sendCommand", () -> onSecondaryOutputsChanged((SelectionChangedEvent<String>) selectionChangedEvent)));
            case WidgetIdentifiers.WIDGET_ID_MASTER_INPUTS ->
                    runLongTask(new LongWork<Void>("selectedMasterInputsChanged", () -> onMasterInputsChanged((SelectionChangedEvent<String>) selectionChangedEvent)));
        }
    }

    private void onChannelChanged(SelectionChangedEvent<Integer> selectionChangedEvent) {
        selectionChangedEvent.getSelectedItems()
                .stream()
                .findFirst()
                .ifPresent(this::changeOutputChannel);
    }

    private void changeOutputChannel(int channel) {
        log.info("Change output channel: {}", channel);
        var model = getModel();
        deviceStateManager.onChannelChanged(model, channel);
        midiRouter.changeOutputChannel(channel);
        refreshPatches();
    }

    private void restoreConfigSelection() {
        midiRouter.setControllerMessageListener(this::onMidiController);
        midiRouter.listenDawOutputs();
        var cfg = configurationService.getProjectConfiguration();
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

    private Void onMasterInputsChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        var cfg = configurationService.getProjectConfiguration();

        List<String> deviceOrPortNames = selectionChangedEvent.getSelectedItems();
        try {
            midiRouter.changeMasterInputs(deviceOrPortNames);
            cfg.setSelectedInputs(deviceOrPortNames);
            configurationService.saveConfig();
        } catch (MidiError e) {
            showError(e);
        }
        return null;
    }

    private Void onSecondaryOutputsChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        var cfg = configurationService.getProjectConfiguration();

        List<String> selectedItems = selectionChangedEvent.getSelectedItems();
        midiRouter.changeSecondaryOutputs(selectedItems);
        cfg.setSelectedSecondaryOutputs(selectedItems);
        configurationService.saveConfig();
        return null;
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
    private Void onModeChanged(SelectionChangedEvent<String> selectionChangedEvent) {
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
            deviceStateManager.onModeChanged(selectedItems.getFirst());
            midiRouter.changeOutputChannel(getModel().getCurrentDeviceState()
                    .getId()
                    .getChannel());
            refreshPatches();
        }
        return null;
    }

    /**
     * Update the view given a selected device and select this device as the main destination for the MIDI router
     */
    private Void onDeviceChanged(SelectionChangedEvent<String> selectionChangedEvent) {
        MidiDeviceDefinition device = getMidiDevice(selectionChangedEvent);
        log.info("Select device: {}", Optional.ofNullable(device)
                .map(MidiDeviceDefinition::getDeviceName)
                .orElse("none"));
        updateProjectConfiguration(device);
        deviceStateManager.onDeviceChanged(device);
        try {
            midiRouter.changeMainDestination(device);
        } catch (MidiError e) {
            log.error("Unexpected error in Midi Router", e);
        }
        refreshPatches();
        return null;
    }

    private void updateProjectConfiguration(MidiDeviceDefinition device) {
        Optional.ofNullable(device)
                .ifPresent(d -> {
                    configurationService.getProjectConfiguration()
                            .setSelectedOutput(d.getDeviceName());
                    configurationService.saveConfig();
                });
    }

    private MidiDeviceDefinition getMidiDevice(SelectionChangedEvent<String> selectionChangedEvent) {
        var cfg = configurationService.getProjectConfiguration();
        return Optional.ofNullable(selectionChangedEvent)
                .map(evt -> selectionChangedEvent.getSelectedItems())
                .filter(selectedItems -> selectedItems.size() == 1)
                .map(selectedItems -> {
                    String deviceName = selectedItems
                            .getFirst();
                    getModel().setSelectedDevice(deviceName);
                    return cfg.getMidiDeviceLibrary()
                            .getDevice(deviceName)
                            .orElseThrow();
                })
                .orElse(null);
    }

    private void refreshPatches() {
        runLaterOnJavaFXThread(() -> {
            patchesManager.refreshPatches();
            deviceStateManager.saveDeviceState();
        });
    }
}
