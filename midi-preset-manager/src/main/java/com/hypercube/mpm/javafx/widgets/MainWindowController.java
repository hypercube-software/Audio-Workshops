package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.event.PatchScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import javafx.fxml.Initializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sound.midi.InvalidMidiDataException;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainWindowController extends Controller<MainWindow, ObservableMainModel> implements Initializable {
    @Autowired
    ProjectConfiguration cfg;
    @Autowired
    ConfigurationFactory configurationFactory;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(ObservableMainModel.getGetInstance());
        getModel().getRoot()
                .setDevices(cfg.getMidiDeviceLibrary()
                        .getDevices()
                        .values()
                        .stream()
                        .map(d -> d.getDeviceName())
                        .sorted()
                        .toList());
        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
    }

    private void onPatchScoreChanged(PatchScoreChangedEvent patchScoreChangedEvent) {
        configurationFactory.updateFavorites(patchScoreChangedEvent.getPatch());
    }

    private void onSearchPatches(SearchPatchesEvent searchPatchesEvent) {
        refreshPatches(getModel().getRoot());
    }

    private void onSelectionChanged(SelectionChangedEvent selectionChangedEvent) {
        log.info(selectionChangedEvent.getDataSource() + " changed ! " + selectionChangedEvent.getSelectedItems()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        MainModel model = getModel().getRoot();
        if (selectionChangedEvent.getDataSource()
                .endsWith(".devices")) {
            onDeviceChanged(selectionChangedEvent, model);
            refreshPatches(model);
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".deviceModes")) {
            onDeviceModeChanged(selectionChangedEvent, model);
            refreshPatches(model);
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".modeCategories")) {
            onCategoriesChanged(selectionChangedEvent, model);
            refreshPatches(model);
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".modeBanks")) {
            onModeBankChanged(selectionChangedEvent, model);
            refreshPatches(model);
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".patches")) {
            onPatchChanged(selectionChangedEvent, model);
        }
    }

    private void onCategoriesChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        List<Integer> selectedItems = selectionChangedEvent.getSelectedItems();
        var state = getCurrentDeviceState(model);
        model.setCurrentSelectedCategories(selectedItems);
        state.setCurrentSelectedCategories(selectedItems);
    }

    private void onModeBankChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceName())
                .orElseThrow();
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            var bankName = model.getModeBanks()
                    .get(selectionChangedEvent.getSelectedItems()
                            .getFirst());
            model.setCurrentModeBankName(bankName);
            getCurrentDeviceState(model)
                    .setCurrentBank(bankName);
        } else {
            model.setCurrentModeBankName(null);
            getCurrentDeviceState(model)
                    .setCurrentBank(null);
        }
    }

    private void onPatchChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {

            Integer patchIndex = selectionChangedEvent.getSelectedItems()
                    .getFirst();
            Patch patch = model.getPatches()
                    .get(patchIndex);
            var device = cfg.getMidiDeviceLibrary()
                    .getDevice(patch.getDevice())
                    .orElseThrow();
            if (patch.getName()
                    .startsWith("@")) {
                File filename = new File(device.getDefinitionFile()
                        .getParent(), "%s/%s/%s/%s".formatted(patch.getDevice(), patch.getMode(), patch.getBank(), patch.getName()
                        .substring(1)));
                MidiPreset midiPreset = MidiPresetBuilder.fromSysExFile(patch.getMode(), patch.getBank(), filename);
                model.getDeviceStates()
                        .get(device.getDeviceName())
                        .getMidiOutDevice()
                        .sendPresetChange(midiPreset);
            } else {
                var values = Arrays.stream(patch.getName()
                                .split("\\|"))
                        .toList();
                String command = values.get(0);
                String name = values.getLast();
                MidiPreset midiPreset = MidiPresetBuilder.parse(device.getDefinitionFile(), 0,
                        device.getPresetFormat(),
                        device.getPresetNumbering(),
                        name,
                        device.getMacros(),
                        List.of(command), List.of(MidiPreset.NO_CC), null);
                model.getDeviceStates()
                        .get(device.getDeviceName())
                        .getMidiOutDevice()
                        .sendPresetChange(midiPreset);
            }
            getCurrentDeviceState(model)
                    .setCurrentPatch(patch);
        }
    }

    private DeviceState getCurrentDeviceState(MainModel model) {
        return model.getDeviceStates()
                .get(model.getCurrentDeviceName());
    }

    private void refreshPatches(MainModel model) {
        if (model.getCurrentDeviceName() == null)
            return;
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceName())
                .orElseThrow();
        List<Patch> patches = List.of();

        String currentModeName = model.getCurrentModeName();
        if (currentModeName != null) {
            // Note: it is possible that currentModeBankName is not yet updated at this point
            // so midiDeviceMode will be null because it points to the previous selected device
            String currentModeBankName = model.getCurrentModeBankName();
            MidiDeviceMode midiDeviceMode = device.getDeviceModes()
                    .get(currentModeName);
            if (midiDeviceMode != null) {
                patches = midiDeviceMode
                        .getBanks()
                        .values()
                        .parallelStream()
                        .filter(bank -> currentModeBankName == null || currentModeBankName
                                .equals(bank.getName()))
                        .flatMap(bank -> bank.getPresets()
                                .stream()
                                .filter(preset -> (model.getCurrentSelectedCategories()
                                        .isEmpty() ||
                                        model.getCurrentSelectedCategories()
                                                .stream()
                                                .map(idx -> model.getModeCategories()
                                                        .get(idx))
                                                .map(c -> c.split(":")[0].trim())
                                                .filter(c -> preset.contains("| " + c + " |") || preset.contains("[" + c + "]"))
                                                .count() > 0) && (model.getCurrentPatchNameFilter() == null || preset.contains(model.getCurrentPatchNameFilter()))
                                )
                                .map(preset -> configurationFactory.getFavorite(new Patch(device.getDeviceName(), currentModeName, bank.getName(), preset, 0)))
                                .filter(patch -> patch.getScore() >= model.getCurrentPatchScoreFilter()))
                        .toList();
            }
        }
        model.setPatches(patches);
        model.setInfo("%d patches".formatted(patches.size()));
        getCurrentDeviceState(model)
                .setCurrentSearchOutput(patches);
    }

    private void onDeviceModeChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceName())
                .orElseThrow();
        var state = getCurrentDeviceState(model);
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            Integer modeIndex = selectionChangedEvent.getSelectedItems()
                    .getFirst();
            var modeName = model.getDeviceModes()
                    .get(modeIndex);
            log.info("Current Mode: " + modeName);
            changeMode(device, state, modeName);
            model.setCurrentModeName(modeName);
            state.setCurrentMode(modeName);
            refreshModeCategoriesAndBanks(model, device, modeName);
        } else {
            model.setModeCategories(List.of());
            state.setCurrentMode(null);
        }
    }

    private void refreshModeCategoriesAndBanks(MainModel model, MidiDeviceDefinition device, String modeName) {
        var mode = device.getDeviceModes()
                .get(modeName);
        if (mode != null) {
            model.setModeCategories(mode.getCategories()
                    .stream()
                    .map(MidiPresetCategory::name)
                    .sorted()
                    .toList());
            model.setModeBanks(mode.getBanks()
                    .keySet()
                    .stream()
                    .sorted()
                    .toList());
        } else {
            model.setModeCategories(null);
            model.setModeBanks(null);
        }
    }

    public void changeMode(MidiDeviceDefinition device, DeviceState state, String modeName) {
        if (state.getCurrentMode() == null || !state.getCurrentMode()
                .equals(modeName)) {
            String modeCommand = device.getDeviceModes()
                    .get(modeName)
                    .getCommand();
            if (modeCommand != null) {
                log.info("Switch to mode: " + modeName);
                var sequences = CommandCall.parse(device.getDefinitionFile(), modeCommand)
                        .stream()
                        .map(commandCall -> {
                            CommandMacro macro = device.getMacro(commandCall);
                            return cfg.getMidiDeviceLibrary()
                                    .forgeMidiRequestSequence(device.getDefinitionFile(), device.getDeviceName(), macro, commandCall);
                        })
                        .toList();
                sequences.forEach(s -> {
                    s.getMidiRequests()
                            .forEach(r -> {
                                try {
                                    List<CustomMidiEvent> requestInstances = SysExBuilder.parse(r.getValue());
                                    requestInstances.forEach(evt -> {
                                        log.info("Send 0x %s to %s".formatted(evt.getHexValuesSpaced(), device.getDeviceName()));
                                        state.getMidiOutDevice()
                                                .send(evt);
                                    });
                                } catch (InvalidMidiDataException e) {
                                    throw new MidiError(e);
                                }
                            });
                });
                state.setCurrentMode(modeName);
            } else {
                log.info("There is no command to switch mode");
            }
        } else {
            log.info("Already in mode: " + modeName);
        }

    }


    private void onDeviceChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        String deviceName = model
                .getDevices()
                .get(selectionChangedEvent.getSelectedItems()
                        .getFirst());
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(deviceName)
                .orElseThrow();
        DeviceState deviceState = model.getDeviceStates()
                .get(deviceName);
        if (deviceState == null) {
            deviceState = new DeviceState();
            deviceState.setDeviceName(deviceName);
            if (deviceState.getMidiOutDevice() == null & device.getOutputMidiDevice() != null) {
                deviceState.setMidiOutDevice(cfg.getMidiDeviceManager()
                        .openOutput(device.getOutputMidiDevice()));
            }
            model.getDeviceStates()
                    .put(deviceName, deviceState);
        }
        model.setCurrentDeviceName(deviceName);
        var modes = device.getDeviceModes()
                .keySet()
                .stream()
                .sorted()
                .toList();
        model.setDeviceModes(modes);
        restoreDeviceState(model, deviceState, device);
    }

    private void restoreDeviceState(MainModel model, DeviceState deviceState, MidiDeviceDefinition device) {
        int currentModeIndex = model.getDeviceModes()
                .indexOf(deviceState.getCurrentMode());
        int currentPatchIndex = deviceState.getCurrentSearchOutput() != null && deviceState.getCurrentPatch() != null ? deviceState.getCurrentSearchOutput()
                .indexOf(deviceState.getCurrentPatch()) : -1;
        log.info("Restore {} state: mode {} bank {} categories {} patch {}", device.getDeviceName(), deviceState.getCurrentMode(), deviceState.getCurrentBank(), deviceState.getCurrentSelectedCategories(), currentPatchIndex);
        model.setCurrentModeName(deviceState.getCurrentMode());
        model.setCurrentModeIndex(currentModeIndex);
        refreshModeCategoriesAndBanks(model, device, deviceState.getCurrentMode());
        model.setCurrentModeBankName(deviceState.getCurrentBank());
        model.setPatches(deviceState.getCurrentSearchOutput());
        model.setCurrentSelectedCategories(deviceState.getCurrentSelectedCategories());
        model.setCurrentPatchIndex(currentPatchIndex);
    }
}
