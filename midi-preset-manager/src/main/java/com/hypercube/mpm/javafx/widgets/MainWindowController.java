package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.config.SelectedPatch;
import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
import com.hypercube.mpm.javafx.event.PatchScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
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
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainWindowController extends Controller<MainWindow, MainModel> implements Initializable {
    @Autowired
    ProjectConfiguration cfg;
    @Autowired
    ConfigurationFactory configurationFactory;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());
        getModel().setDevices(cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .map(d -> d.getDeviceName())
                .sorted()
                .toList());
        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
        addEventListener(FilesDroppedEvent.class, this::onFilesDropped);
        cfg.getSelectedPatches()
                .forEach((deviceName, selectedPatch) -> initDeviceState(getModel(), deviceName));
    }

    private void onFilesDropped(FilesDroppedEvent filesDroppedEvent) {
        try {
            MainModel model = getModel();
            var state = getCurrentDeviceState(model);
            if (state != null) {
                filesDroppedEvent.getFiles()
                        .forEach(f -> cfg.getMidiDeviceLibrary()
                                .importSysex(state.getDeviceName(), state.getCurrentMode(), f));
                var device = cfg.getMidiDeviceLibrary()
                        .getDevice(model
                                .getCurrentDeviceName())
                        .orElseThrow();
                cfg.getMidiDeviceLibrary()
                        .collectCustomPatches(device);
                refreshModeCategoriesAndBanks(model, device, model.getCurrentModeName());
            }
        } catch (Exception e) {
            log.error("Unexpected error:", e);
        }
    }

    private void onPatchScoreChanged(PatchScoreChangedEvent patchScoreChangedEvent) {
        configurationFactory.updateFavorites(patchScoreChangedEvent.getPatch());
    }

    private void onSearchPatches(SearchPatchesEvent searchPatchesEvent) {
        refreshPatches(getModel());
    }

    private void onSelectionChanged(SelectionChangedEvent selectionChangedEvent) {
        log.info(selectionChangedEvent.getDataSource() + " changed ! " + selectionChangedEvent.getSelectedItems()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        MainModel model = getModel();
        if (selectionChangedEvent.getDataSource()
                .endsWith(".devices")) {
            onDeviceChanged(selectionChangedEvent, model);
            refreshPatches(model);
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".deviceModes")) {
            onModeChanged(selectionChangedEvent, model);
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
        if (state != null) {
            state.setCurrentSelectedCategories(selectedItems);
        }
        model.setCurrentSelectedCategories(selectedItems);
    }

    private void onModeBankChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
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
            if (!patch.getName()
                    .equals(getCurrentDeviceState(model).getCurrentPatchName())) {
                sendPatchToDevice(model, patch);
            }
            setCurrentPatch(model, patch);
        }
    }

    private void sendPatchToDevice(MainModel model, Patch patch) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(patch.getDevice())
                .orElseThrow();
        log.info("Send patch '{}' to '{}'", patch.getName(), patch.getDevice());
        MidiOutDevice port = model.getDeviceStates()
                .get(device.getDeviceName())
                .getMidiOutDevice();
        if (patch.getCommand()
                .startsWith("@")) {
            String sysExFilename = patch.getCommand()
                    .substring(1);
            File filename = new File(device.getDefinitionFile()
                    .getParent(), "%s/%s/%s/%s".formatted(patch.getDevice(), patch.getMode(), patch.getBank(), sysExFilename));
            if (filename.exists()) {
                MidiPreset midiPreset = MidiPresetBuilder.fromSysExFile(patch.getMode(), patch.getBank(), filename);
                if (port != null) {
                    port.sendPresetChange(midiPreset);
                }
            }
        } else {
            MidiPreset midiPreset = MidiPresetBuilder.parse(device.getDefinitionFile(), 0,
                    device.getPresetFormat(),
                    device.getPresetNumbering(),
                    patch.getName(),
                    device.getMacros(),
                    List.of(patch.getCommand()), List.of(MidiPreset.NO_CC), null);
            if (port != null) {
                port.sendPresetChange(midiPreset);
            }
        }
    }

    private void setCurrentPatch(MainModel model, Patch patch) {
        getCurrentDeviceState(model).setCurrentPatchName(patch.getName());
        cfg.getSelectedPatches()
                .put(patch.getDevice(), new SelectedPatch(patch.getMode(), patch.getBank(), patch.getName(), patch.getCategory(), patch.getCommand()));
        configurationFactory.saveConfig(cfg);
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
                                .filter(preset -> patchCategoryMatches(model, preset) && patchNameMatches(model, preset))
                                .map(preset -> configurationFactory.getFavorite(forgePatch(device.getDeviceName(), currentModeName, bank.getName(), preset)))
                                .filter(patch -> patchScoreMatches(model, patch)))
                        .sorted(Comparator.comparing(Patch::getName))
                        .toList();
            }
        }
        model.setPatches(patches);
        model.setInfo("%d patches".formatted(patches.size()));
        DeviceState deviceState = getCurrentDeviceState(model);
        deviceState
                .setCurrentSearchOutput(patches);
        selectCurrentPatch(model, deviceState);
    }

    private Patch forgePatch(String deviceName, String currentModeName, String bankName, String patchDefinition) {
        final String command;
        final String category;
        final String name;
        if (patchDefinition.startsWith("@")) {
            List<String> parts = Arrays.stream(patchDefinition.split("\\[|\\]"))
                    .toList();
            command = patchDefinition.trim();
            if (parts.size() == 3) {
                category = parts.get(1)
                        .trim();
                name = parts.get(2)
                        .trim();
            } else {
                category = null;
                name = patchDefinition.substring(1)
                        .trim();
            }
        } else {
            List<String> parts = Arrays.stream(patchDefinition.split("\\|"))
                    .toList();
            command = parts.get(0)
                    .trim();
            category = parts.get(1)
                    .trim();
            name = parts.get(2)
                    .trim();
        }
        return new Patch(deviceName, currentModeName, bankName, name, category, command, 0);
    }

    private static boolean patchScoreMatches(MainModel model, Patch patch) {
        return patch.getScore() >= model.getCurrentPatchScoreFilter();
    }

    private static boolean patchNameMatches(MainModel model, String preset) {
        return model.getCurrentPatchNameFilter() == null || preset.contains(model.getCurrentPatchNameFilter());
    }

    private static boolean patchCategoryMatches(MainModel model, String preset) {
        return model.getCurrentSelectedCategories()
                .isEmpty() ||
                model.getCurrentSelectedCategories()
                        .stream()
                        .map(idx -> model.getModeCategories()
                                .get(idx))
                        .map(c -> c.split(":")[0].trim())
                        .filter(c -> preset.contains("| " + c + " |") || preset.contains("[" + c + "]"))
                        .count() > 0;
    }

    private void selectCurrentPatch(MainModel model, DeviceState deviceState) {
        List<Patch> patches = model.getPatches();

        if (deviceState.getCurrentPatchName() != null) {
            for (int idx = 0; idx < patches.size(); idx++) {
                if (patches.get(idx)
                        .getName()
                        .equals(deviceState.getCurrentPatchName())) {
                    model.setCurrentPatchIndex(idx);
                    break;
                }
            }
        } else {
            model.setCurrentPatchIndex(-1);
        }
    }

    private void onModeChanged(SelectionChangedEvent selectionChangedEvent, MainModel model) {
        if (model.getCurrentDeviceName() == null)
            return;

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
            model.setCurrentSelectedCategories(List.of());
            state.setCurrentMode(modeName);
            state.setCurrentBank(null);
            state.setCurrentSearchOutput(null);
            refreshModeCategoriesAndBanks(model, device, modeName);
        } else {
            model.setModeCategories(List.of());
            state.setCurrentBank(null);
            state.setCurrentSearchOutput(null);
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
            deviceState = initDeviceState(model, deviceName);
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

    private DeviceState initDeviceState(MainModel model, String deviceName) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(deviceName)
                .orElseThrow();
        DeviceState deviceState;
        deviceState = new DeviceState();
        deviceState.setDeviceName(deviceName);
        SelectedPatch selectedPatch = cfg.getSelectedPatches()
                .get(deviceName);
        if (deviceState.getMidiOutDevice() == null & device.getOutputMidiDevice() != null) {
            try {
                deviceState.setMidiOutDevice(cfg.getMidiDeviceManager()
                        .openOutput(device.getOutputMidiDevice()));
            } catch (MidiError e) {
                log.error("Unable to open device " + device.getOutputMidiDevice(), e);
            }
        }
        model.getDeviceStates()
                .put(deviceName, deviceState);
        if (selectedPatch != null) {
            deviceState.setCurrentMode(selectedPatch.getMode());
            deviceState.setCurrentPatchName(selectedPatch.getName());
            sendPatchToDevice(getModel(), forgePatch(deviceName, selectedPatch));

        }
        return deviceState;
    }

    private Patch forgePatch(String deviceName, SelectedPatch selectedPatch) {
        return new Patch(deviceName, selectedPatch.getMode(), selectedPatch.getBank(), selectedPatch.getName(), null, selectedPatch.getCommand(), 0);
    }

    private void restoreDeviceState(MainModel model, DeviceState deviceState, MidiDeviceDefinition device) {
        log.info("Restore {} state: mode '{}' bank '{}' categories '{}' patch '{}'", device.getDeviceName(), deviceState.getCurrentMode(), deviceState.getCurrentBank(), deviceState.getCurrentSelectedCategories(), deviceState.getCurrentPatchName());
        int currentModeIndex = model.getDeviceModes()
                .indexOf(deviceState.getCurrentMode());
        model.setCurrentModeName(deviceState.getCurrentMode());
        model.setCurrentModeIndex(currentModeIndex);
        refreshModeCategoriesAndBanks(model, device, deviceState.getCurrentMode());
        model.setCurrentModeBankName(deviceState.getCurrentBank());
        model.setPatches(deviceState.getCurrentSearchOutput());
        model.setCurrentSelectedCategories(deviceState.getCurrentSelectedCategories());
        model.setCurrentPatchIndex(-1);
    }


}
