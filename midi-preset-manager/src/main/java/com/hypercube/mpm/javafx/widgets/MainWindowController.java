package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.mpm.app.PatchesManager;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
import com.hypercube.mpm.javafx.event.PatchScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.midi.MidiRouter;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.importer.PatchImporter;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainWindowController extends Controller<MainWindow, MainModel> implements Initializable {
    @Autowired
    ProjectConfiguration cfg;
    @Autowired
    MidiRouter midiRouter;
    @Autowired
    DeviceStateManager deviceStateManager;
    @Autowired
    PatchesManager patchesManager;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());
        MainModel model = getModel();
        model.setDevices(buildDeviceList());
        model.setMidiInPorts(buildMidiInPortsList());
        model.setMidiThruPorts(buildMidiThruPortsList());
        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
        addEventListener(FilesDroppedEvent.class, this::onFilesDropped);
        initDevices();
    }

    /**
     * Restore the state of all devices when the application start
     */
    private void initDevices() {
        cfg.getSelectedPatches()
                .forEach(selectedPatch ->
                {
                    deviceStateManager.initDeviceStateWithPatch(selectedPatch);
                    patchesManager.sendPatchToDevice(selectedPatch);
                });
        onDeviceChanged(null);
        midiRouter.setControllerMessageListener(this::onMidiController);
        midiRouter.listenDawOutputs();
    }

    private void onPatchScoreChanged(PatchScoreChangedEvent patchScoreChangedEvent) {
        patchesManager.onPatchScoreChanged(patchScoreChangedEvent.getPatch());
    }

    private void onMidiController(String s) {
        Platform.runLater(() -> getModel().setEventInfo(s));
    }

    /**
     * When possible we replace the MIDI port name by a known device
     */
    private List<String> buildMidiInPortsList() {
        return cfg.getMidiDeviceManager()
                .getInputs()
                .stream()
                .map(port -> {
                    for (var device : cfg.getMidiDeviceLibrary()
                            .getDevices()
                            .values()) {
                        if (port.getName()
                                .equals(device.getInputMidiDevice())) {
                            return device.getDeviceName();
                        }
                    }
                    return port.getName();
                })
                .sorted()
                .toList();
    }

    /**
     * When possible we replace the MIDI port name by a known device
     */
    private List<String> buildMidiThruPortsList() {
        return cfg.getMidiDeviceManager()
                .getOutputs()
                .stream()
                .map(port -> {
                    for (var device : cfg.getMidiDeviceLibrary()
                            .getDevices()
                            .values()) {
                        if (port.getName()
                                .equals(device.getOutputMidiDevice())) {
                            return device.getDeviceName();
                        }
                    }
                    return port.getName();
                })
                .sorted()
                .toList();
    }

    private List<String> buildDeviceList() {
        return cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .map(MidiDeviceDefinition::getDeviceName)
                .sorted()
                .toList();
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
            case WidgetIdentifiers.WIDGET_ID_DEVICE -> onDeviceChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_MODE -> onModeChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_MODE_CHANNEL -> onChannelChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_CATEGORY -> onCategoriesChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_BANK -> onBankChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PATCH -> onPatchChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_PASSTHRU_OUTPUTS -> onPassThruChanged(selectionChangedEvent);
            case WidgetIdentifiers.WIDGET_ID_MASTER_INPUTS -> onMasterInputChanged(selectionChangedEvent);
        }
    }

    private void onChannelChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        selectionChangedEvent.getSelectedItems()
                .stream()
                .map(Integer.class::cast)
                .findFirst()
                .ifPresent(channel -> {
                    deviceStateManager.onChannelChanged(model, channel);
                    midiRouter.changeOutputChannel(channel);
                });
        refreshPatches();
    }

    private void onMasterInputChanged(SelectionChangedEvent selectionChangedEvent) {
        String deviceOrPortName = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
        midiRouter.changeMainSource(deviceOrPortName);
    }

    private void onPassThruChanged(SelectionChangedEvent selectionChangedEvent) {
        List<String> selectedItems = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(String.class::cast)
                .toList();
        midiRouter.changeSecondaryOutputs(selectedItems);
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
    private void onDeviceChanged(SelectionChangedEvent selectionChangedEvent) {
        MidiDeviceDefinition device = Optional.ofNullable(selectionChangedEvent)
                .map(evt -> selectionChangedEvent.getSelectedIndexes())
                .map(selectedIndexes -> {
                    String deviceName = getModel()
                            .getDevices()
                            .get(selectedIndexes
                                    .getFirst());
                    return cfg.getMidiDeviceLibrary()
                            .getDevice(deviceName)
                            .orElseThrow();
                })
                .orElse(null);
        deviceStateManager.onDeviceChanged(device);
        midiRouter.changeMainDestination(device);
        refreshPatches();
    }

    private void refreshPatches() {
        patchesManager.refreshPatches();
        deviceStateManager.saveDeviceState();
    }
}
