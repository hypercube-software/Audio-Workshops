package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.config.ProjectConfiguration;
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
            model.setCurrentSelectedCategories(selectionChangedEvent.getSelectedItems());
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
        } else {
            model.setCurrentModeBankName(null);
        }
    }

    private void onPatchChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {

            Patch patch = model.getPatches()
                    .get(selectionChangedEvent.getSelectedItems()
                            .getFirst());
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
        }
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
            String currentModeBankName = model.getCurrentModeBankName();
            patches = device.getDeviceModes()
                    .get(currentModeName)
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
                            .map(preset -> new Patch(device.getDeviceName(), currentModeName, bank.getName(), preset, 0)))
                    .toList();
        }
        model.setPatches(patches);
    }

    private void onDeviceModeChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceName())
                .orElseThrow();
        var state = model.getDeviceStates()
                .get(model.getCurrentDeviceName());
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            var modeName = model.getDeviceModes()
                    .get(selectionChangedEvent.getSelectedItems()
                            .getFirst());
            changeMode(device, state, modeName);
            model.setCurrentModeName(modeName);
            var mode = device.getDeviceModes()
                    .get(modeName);
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
            model.setModeCategories(List.of());
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
        model
                .setDeviceModes(modes);
    }
}
