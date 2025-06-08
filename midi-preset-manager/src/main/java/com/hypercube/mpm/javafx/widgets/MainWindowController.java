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
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.library.importer.PatchImporter;
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
                .map(MidiDeviceDefinition::getDeviceName)
                .sorted()
                .toList());
        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
        addEventListener(FilesDroppedEvent.class, this::onFilesDropped);
        cfg.getSelectedPatches()
                .forEach(selectedPatch ->
                {
                    initDeviceStateFromConfig(selectedPatch.getDevice(), selectedPatch);
                    sendPatchToDevice(selectedPatch);
                });
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
                                .getDeviceName())
                        .orElseThrow();
                filesDroppedEvent.getFiles()
                        .forEach(f -> patchImporter.importSysex(device, state.getCurrentMode(), f));

                midiDeviceLibrary
                        .collectCustomPatches(device);
                refreshModeCategoriesAndBanks(model, device, model.getCurrentDeviceState()
                        .getCurrentMode());
            }
        } catch (Exception e) {
            log.error("Unexpected error:", e);
        }
    }

    private void onPatchScoreChanged(PatchScoreChangedEvent patchScoreChangedEvent) {
        configurationFactory.updateFavorites(patchScoreChangedEvent.getPatch());
    }

    private void onSearchPatches(SearchPatchesEvent searchPatchesEvent) {
        refreshPatches();
    }

    private void onSelectionChanged(SelectionChangedEvent selectionChangedEvent) {
        log.info(selectionChangedEvent.getDataSource() + " changed ! " + selectionChangedEvent.getSelectedIndexes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        if (selectionChangedEvent.getDataSource()
                .endsWith(".devices")) {
            onDeviceChanged(selectionChangedEvent);
            refreshPatches();
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".deviceModes")) {
            onModeChanged(selectionChangedEvent);
            refreshPatches();
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".modeCategories")) {
            onCategoriesChanged(selectionChangedEvent);
            refreshPatches();
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".modeBanks")) {
            onBankChanged(selectionChangedEvent);
            refreshPatches();
        } else if (selectionChangedEvent.getDataSource()
                .endsWith(".patches")) {
            onPatchChanged(selectionChangedEvent);
        }
    }

    private void onCategoriesChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        List<String> selectedItems = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(Object::toString)
                .toList();
        var state = model.getCurrentDeviceState();
        if (state != null) {
            state.setCurrentSelectedCategories(selectedItems);
        }
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
    }

    private void onPatchChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {

            Patch patch = (Patch) selectionChangedEvent.getSelectedItems()
                    .getFirst();
            if (!patch.equals(model.getCurrentDeviceState()
                    .getCurrentPatch())) {
                setCurrentPatch(patch);
            }
        }
    }

    private void sendPatchToDevice(SelectedPatch selectedPatch) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(selectedPatch.getDevice())
                .orElseThrow();
        log.info("Send patch '{}' to '{}'", selectedPatch.getName(), selectedPatch.getDevice());
        MidiOutDevice port = getModel().getDeviceStates()
                .get(device.getDeviceName())
                .getMidiOutDevice();
        if (selectedPatch.getCommand()
                .startsWith("@")) {
            String sysExFilename = selectedPatch.getCommand()
                    .substring(1);
            File filename = new File(device.getDefinitionFile()
                    .getParent(), "%s/%s/%s/%s".formatted(selectedPatch.getDevice(), selectedPatch.getMode(), selectedPatch.getBank(), sysExFilename));
            if (filename.exists()) {
                MidiPreset midiPreset = MidiPresetBuilder.fromSysExFile(selectedPatch.getMode(), selectedPatch.getBank(), filename);
                if (port != null) {
                    port.sendPresetChange(midiPreset);
                }
            } else {
                log.error("Patch file no longer exists: " + filename.getAbsolutePath());
            }
        } else {
            MidiPreset midiPreset = MidiPresetBuilder.parse(device.getDefinitionFile(), 0,
                    device.getPresetFormat(),
                    device.getPresetNumbering(),
                    selectedPatch.getName(),
                    device.getMacros(),
                    List.of(selectedPatch.getCommand()), List.of(MidiPreset.NO_CC), null);
            if (port != null) {
                port.sendPresetChange(midiPreset);
            }
        }
    }

    private void setCurrentPatch(Patch patch) {
        getModel().getCurrentDeviceState()
                .setCurrentPatch(patch);
        SelectedPatch selectedPatch = new SelectedPatch(patch.getDevice(), patch.getMode(), patch.getBank(), patch.getName(), patch.getCategory(), patch.getCommand());
        saveSelectedPatchToConfig(selectedPatch);
        sendPatchToDevice(selectedPatch);
        getModel().getCurrentDeviceState()
                .setSelectedPatch(selectedPatch);
        saveDeviceState();
    }

    private void saveSelectedPatchToConfig(SelectedPatch patch) {
        var list = cfg.getSelectedPatches()
                .stream()
                .filter(sp -> !sp.getDevice()
                        .equals(patch.getDevice()))
                .collect(Collectors.toList());
        list.add(patch);
        cfg.setSelectedPatches(list);
        configurationFactory.saveConfig(cfg);
    }

    private void refreshPatches() {
        var model = getModel();
        if (model.getCurrentDeviceState()
                .getDeviceName() == null)
            return;
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceState()
                        .getDeviceName())
                .orElseThrow();
        List<Patch> patches = List.of();

        String currentModeName = model.getCurrentDeviceState()
                .getCurrentMode();
        if (currentModeName != null) {
            // Note: it is possible that currentModeBankName is not yet updated at this point
            // so midiDeviceMode will be null because it points to the previous selected device
            String currentModeBankName = model.getCurrentDeviceState()
                    .getCurrentBank();
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
                                .filter(preset -> patchCategoryMatches(preset) && patchNameMatches(preset))
                                .map(preset -> configurationFactory.getFavorite(forgePatch(device.getDeviceName(), currentModeName, bank.getName(), preset)))
                                .filter(this::patchScoreMatches))
                        .sorted(Comparator.comparing(Patch::getName))
                        .toList();
            }
        }
        model.setInfo("%s | MIDI OUT '%s' | %d patches".formatted(device.getDeviceName(), device.getOutputMidiDevice(), patches.size()));
        DeviceState deviceState = model.getCurrentDeviceState();
        deviceState
                .setCurrentSearchOutput(patches);
        selectCurrentPatch(deviceState);
        saveDeviceState();
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

    private boolean patchScoreMatches(Patch patch) {
        return patch.getScore() >= getModel().getCurrentPatchScoreFilter();
    }

    private boolean patchNameMatches(String preset) {
        var model = getModel();
        return model.getCurrentPatchNameFilter() == null || preset.contains(model.getCurrentPatchNameFilter());
    }

    private boolean patchCategoryMatches(String preset) {
        var model = getModel();
        return model.getCurrentDeviceState()
                .getCurrentSelectedCategories()
                .isEmpty() ||
                model.getCurrentDeviceState()
                        .getCurrentSelectedCategories()
                        .stream()
                        .map(c -> c.split(":")[0].trim())
                        .anyMatch(c -> preset.contains("| " + c + " |") || preset.contains("[" + c + "]"));
    }

    private void selectCurrentPatch(DeviceState deviceState) {
        Patch selectedPatch = deviceState.getCurrentSearchOutput()
                .stream()
                .filter(p -> p.sameAs(deviceState.getSelectedPatch()))
                .findFirst()
                .orElse(null);
        if (selectedPatch == null && deviceState.getSelectedPatch() != null) {
            log.warn("Patch not found in search result: " + deviceState.getSelectedPatch()
                    .getName());
        }
        deviceState.setCurrentPatch(selectedPatch);
    }

    private void onModeChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        if (model.getCurrentDeviceState()
                .getDeviceName() == null)
            return;

        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceState()
                        .getDeviceName())
                .orElseThrow();
        var state = model.getCurrentDeviceState();
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            String modeName = (String) selectionChangedEvent.getSelectedItems()
                    .getFirst();
            log.info("Current Mode: " + modeName);
            changeMode(device, state, modeName);
            refreshModeCategoriesAndBanks(model, device, modeName);
            state.setCurrentSelectedCategories(List.of());
            state.setCurrentMode(modeName);
            state.setCurrentBank(null);
            state.setCurrentSearchOutput(null);
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
                sequences.forEach(s -> s.getMidiRequests()
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
                        }));
                state.setCurrentMode(modeName);
            } else {
                log.info("There is no command to switch mode");
            }
        } else {
            log.info("Already in mode: " + modeName);
        }

    }


    private void onDeviceChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        String deviceName = model
                .getDevices()
                .get(selectionChangedEvent.getSelectedIndexes()
                        .getFirst());
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(deviceName)
                .orElseThrow();
        if (!model.getDeviceStates()
                .containsKey(deviceName)) {
            initDeviceStateFromConfig(deviceName, null);
        }
        var modes = device.getDeviceModes()
                .keySet()
                .stream()
                .sorted()
                .toList();
        model.setDeviceModes(modes);
        restoreDeviceState(deviceName, device);
    }

    private void dumpStates() {
        getModel().getDeviceStates()
                .forEach(
                        (key, source) -> log.info("[{}/{}] state: mode '{}' bank '{}' categories '{}' patch '{}'",
                                key, source.getDeviceName(), source.getCurrentMode(), source.getCurrentBank(), source.getCurrentSelectedCategories(), source.getSelectedPatch()
                        ));
    }

    private void saveDeviceState() {
        var model = getModel();
        //dumpStates();
        var source = model.getCurrentDeviceState();
        if (source.getDeviceName() != null) {
            log.info("Save {} state: mode '{}' bank '{}' categories '{}' patch '{}'", source.getDeviceName(), source.getCurrentMode(), source.getCurrentBank(), source.getCurrentSelectedCategories(), source.getSelectedPatch()
            );
            var target = model.getDeviceStates()
                    .get(source.getDeviceName());
            if (!target.getDeviceName()
                    .equals(source.getDeviceName())) {
                throw new IllegalStateException();
            }
            copyState(source, target);
        } else {
            log.info("Nothing to save, no device selected");
        }
        dumpStates();
    }

    private void restoreDeviceState(DeviceState source, MainModel target) {
        var current = target.getCurrentDeviceState();
        log.info("Restore {} state: mode '{}' bank '{}' categories '{}' patch '{}'", source.getDeviceName(), source.getCurrentMode(), source.getCurrentBank(), source.getCurrentSelectedCategories(), source.getSelectedPatch()
        );
        log.info("Over    {} state: mode '{}' bank '{}' categories '{}' patch '{}'", current.getDeviceName(), current.getCurrentMode(), source.getCurrentBank(), current.getCurrentSelectedCategories(), current.getSelectedPatch()
        );
        copyState(source, current);
    }

    private void copyState(DeviceState source, DeviceState target) {
        if (target != null) {
            target.setDeviceName(source.getDeviceName());
            target.setCurrentMode(source.getCurrentMode());
            target.setCurrentBank(source.getCurrentBank());
            target.setMidiOutDevice(source.getMidiOutDevice());
            target.setCurrentSelectedCategories(source.getCurrentSelectedCategories());
            target.setCurrentSearchOutput(source.getCurrentSearchOutput());
            target.setCurrentPatch(source.getCurrentPatch());
            target.setSelectedPatch(source.getSelectedPatch());
        }
    }

    private void initDeviceStateFromConfig(String deviceName, SelectedPatch selectedPatch) {

        var device = cfg.getMidiDeviceLibrary()
                .getDevice(deviceName)
                .orElseThrow();
        DeviceState deviceState;
        deviceState = new DeviceState();
        deviceState.setDeviceName(deviceName);
        if (deviceState.getMidiOutDevice() == null & device.getOutputMidiDevice() != null) {
            try {
                deviceState.setMidiOutDevice(cfg.getMidiDeviceManager()
                        .openOutput(device.getOutputMidiDevice()));
            } catch (MidiError e) {
                log.error("Unable to open device " + device.getOutputMidiDevice(), e);
            }
        }
        getModel().getDeviceStates()
                .put(deviceState.getDeviceName(), deviceState);
        if (selectedPatch != null) {
            deviceState.setCurrentMode(selectedPatch.getMode());
            deviceState.setSelectedPatch(selectedPatch);
        }
    }

    private void restoreDeviceState(String deviceName, MidiDeviceDefinition device) {
        var model = getModel();
        DeviceState deviceState = model.getDeviceStates()
                .get(deviceName);
        refreshModeCategoriesAndBanks(model, device, deviceState.getCurrentMode());
        restoreDeviceState(deviceState, model);
    }


}
