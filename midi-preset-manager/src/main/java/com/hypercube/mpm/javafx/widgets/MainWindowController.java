package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
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
import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDevicePreset;
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
import java.util.*;
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
        List<MidiPresetCategory> selectedItems = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(obj -> (MidiPresetCategory) obj)
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

    private void sendPatchToDevice(Patch selectedPatch) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(selectedPatch.getDevice())
                .orElseThrow();
        log.info("Send patch '{}' to '{}'", selectedPatch.getName(), selectedPatch.getDevice());
        MidiOutDevice port = getModel().getDeviceStates()
                .get(device.getDeviceName())
                .getMidiOutDevice();
        if (selectedPatch.getFilename() != null) {
            File filename = new File(device.getDefinitionFile()
                    .getParent(), "%s/%s/%s/%s".formatted(selectedPatch.getDevice(), selectedPatch.getMode(), selectedPatch.getBank(), selectedPatch.getFilename()));
            if (filename.exists()) {
                log.info(filename.getAbsolutePath());
                MidiPreset midiPreset = MidiPresetBuilder.fromSysExFile(selectedPatch.getMode(), selectedPatch.getBank(), filename);
                if (port != null) {
                    selectEditBuffer(selectedPatch, device, port);
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

    /**
     * Some devices need to first select a factory patch before sending a sysex Edit Buffer update
     */
    private void selectEditBuffer(Patch selectedPatch, MidiDeviceDefinition device, MidiOutDevice port) {
        if (selectedPatch.getCommand() != null) {
            List<Integer> digits = forgeCommand(device.getPresetFormat(), selectedPatch.getCommand());
            log.info("Select Edit Buffer with preset format %s: %s".formatted(device.getPresetFormat(), digits.stream()
                    .map(v -> "$%02X".formatted(v))
                    .collect(Collectors.joining(","))));
            switch (device.getPresetFormat()) {
                case NO_BANK_PRG -> {
                    port.sendProgramChange(digits.getLast());
                }
                case BANK_MSB_PRG -> {
                    port.sendBankMSB(digits.getFirst());
                    port.sendProgramChange(digits.getLast());
                }
                case BANK_LSB_PRG -> {
                    port.sendBankLSB(digits.getFirst());
                    port.sendProgramChange(digits.getLast());
                }
                case BANK_MSB_LSB_PRG -> {
                    port.sendBankMSB(digits.getFirst());
                    port.sendBankLSB(digits.get(1));
                    port.sendProgramChange(digits.getLast());
                }
                case BANK_PRG_PRG -> {
                    port.sendProgramChange(digits.getFirst());
                    port.sendProgramChange(digits.getLast());
                }
            }
        }
    }

    public static List<Integer> forgeCommand(MidiBankFormat presetFormat, String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return Collections.emptyList();
        }

        // La chaîne doit avoir une longueur paire pour représenter des octets complets
        if (hexString.length() % 2 != 0) {
            // Vous pourriez choisir de lever une IllegalArgumentException ici
            // ou de logger un avertissement, selon la robustesse souhaitée.
            log.warn("La chaîne hexadécimale '{}' a une longueur impaire et ne peut pas être convertie.", hexString);
            return Collections.emptyList();
        }

        List<Integer> byteValues = new ArrayList<>();
        for (int i = 0; i < hexString.length(); i += 2) {
            String byteStr = hexString.substring(i, i + 2);
            // Integer.parseInt avec une base de 16 convertit l'hexadécimal en entier
            int byteValue = Integer.parseInt(byteStr, 16);
            byteValues.add(byteValue);
        }
        int expectedSize = switch (presetFormat) {
            case NO_BANK_PRG -> 1;
            case BANK_MSB_PRG -> 2;
            case BANK_LSB_PRG -> 2;
            case BANK_MSB_LSB_PRG -> 3;
            case BANK_PRG_PRG -> 2;
        };
        if (byteValues.size() != expectedSize) {
            throw new MidiConfigError("Unexpected command size given preset format '%s' for patch '%s', expected %d digits, got %d".formatted(presetFormat, hexString, expectedSize, byteValues.size()));
        }
        return byteValues;
    }

    private void setCurrentPatch(Patch patch) {
        getModel().getCurrentDeviceState()
                .setCurrentPatch(patch);
        saveSelectedPatchToConfig(patch);
        sendPatchToDevice(patch);
        saveDeviceState();
    }

    private void saveSelectedPatchToConfig(Patch patch) {
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
                // Patches are stored by banks as String to keep space, we call them "presets"
                // Once filtered, we convert string presets to a class Patch with score 0
                // Then the score is updated configurationFactory.getFavorite()
                // Finally they are sorted by name
                patches = midiDeviceMode
                        .getBanks()
                        .values()
                        .parallelStream()
                        .filter(bank -> currentModeBankName == null || currentModeBankName
                                .equals(bank.getName()))
                        .flatMap(bank -> bank.getPresets()
                                .stream()
                                .filter(preset -> patchCategoryMatches(preset) && patchNameMatches(preset))
                                .map(preset ->
                                        configurationFactory.getFavorite(forgePatch(device.getDeviceName(), currentModeName, bank.getName(), preset)))
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

    private Patch forgePatch(String deviceName, String currentModeName, String bankName, MidiDevicePreset preset) {
        return new Patch(deviceName, currentModeName, bankName, preset.name(), preset.category(), preset.command(), preset.filename(), 0);
    }

    private boolean patchScoreMatches(Patch patch) {
        return patch.getScore() >= getModel().getCurrentPatchScoreFilter();
    }

    private boolean patchNameMatches(MidiDevicePreset preset) {
        var model = getModel();
        return model.getCurrentPatchNameFilter() == null || preset.name()
                .contains(model.getCurrentPatchNameFilter());
    }

    private boolean patchCategoryMatches(MidiDevicePreset preset) {
        var model = getModel();
        return model.getCurrentDeviceState()
                .getCurrentSelectedCategories()
                .isEmpty() ||
                model.getCurrentDeviceState()
                        .getCurrentSelectedCategories()
                        .stream()
                        .anyMatch(c -> c.name()
                                .equals(preset.category()));
    }

    private void selectCurrentPatch(DeviceState deviceState) {
        Patch selectedPatch = deviceState.getCurrentSearchOutput()
                .stream()
                .filter(p -> p.equals(deviceState.getCurrentPatch()))
                .findFirst()
                .orElse(null);
        if (selectedPatch == null && deviceState.getCurrentPatch() != null) {
            log.warn("Patch not found in search result: " + deviceState.getCurrentPatch()
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
                    .sorted(Comparator.comparing(MidiPresetCategory::name))
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
            MidiOutDevice midiOutDevice = state.getMidiOutDevice();
            if (modeCommand != null && midiOutDevice != null) {
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
                                    midiOutDevice
                                            .send(evt);
                                });
                            } catch (InvalidMidiDataException e) {
                                throw new MidiError(e);
                            }
                        }));
                state.setCurrentMode(modeName);
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
                        (key, source) -> log.info("[{}/{}] state: mode '{}' command '{}' categories '{}' patch '{}'",
                                key, source.getDeviceName(), source.getCurrentMode(), source.getCurrentBank(), source.getCurrentSelectedCategories(), source.getCurrentPatch()
                        ));
    }

    private void saveDeviceState() {
        var model = getModel();
        //dumpStates();
        var source = model.getCurrentDeviceState();
        if (source.getDeviceName() != null) {
            log.info("Save {} state: mode '{}' command '{}' categories '{}' patch '{}'", source.getDeviceName(), source.getCurrentMode(), source.getCurrentBank(), source.getCurrentSelectedCategories(), source.getCurrentPatch()
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
        log.info("Restore {} state: mode '{}' command '{}' categories '{}' patch '{}'", source.getDeviceName(), source.getCurrentMode(), source.getCurrentBank(), source.getCurrentSelectedCategories(), source.getCurrentPatch());
        log.info("Over    {} state: mode '{}' command '{}' categories '{}' patch '{}'", current.getDeviceName(), current.getCurrentMode(), source.getCurrentBank(), current.getCurrentSelectedCategories(), current.getCurrentPatch());
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
        }
    }

    private void initDeviceStateFromConfig(String deviceName, Patch selectedPatch) {

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
            deviceState.setCurrentPatch(selectedPatch);
        }
    }

    private void restoreDeviceState(String deviceName, MidiDeviceDefinition device) {
        var model = getModel();
        DeviceState deviceState = model.getDeviceStates()
                .get(deviceName);
        if (device.getDeviceModes()
                .size() == 1 && deviceState
                .getCurrentMode() == null) {
            deviceState
                    .setCurrentMode(device.getDeviceModes()
                            .values()
                            .stream()
                            .toList()
                            .getFirst()
                            .getName());
        }
        refreshModeCategoriesAndBanks(model, device, deviceState.getCurrentMode());
        restoreDeviceState(deviceState, model);
    }


}
