package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
import com.hypercube.mpm.javafx.event.PatchScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.midi.MidiRouter;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.DeviceStateId;
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
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDevicePreset;
import com.hypercube.workshop.midiworkshop.common.sysex.library.importer.PatchImporter;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import javafx.application.Platform;
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
    @Autowired
    MidiRouter midiRouter;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());
        getModel().setDevices(buildDeviceList());
        getModel().setMidiInPorts(buildMidiInPortsList());
        getModel().setMidiThruPorts(buildMidiThruPortsList());
        addEventListener(SelectionChangedEvent.class, this::onSelectionChanged);
        addEventListener(SearchPatchesEvent.class, this::onSearchPatches);
        addEventListener(PatchScoreChangedEvent.class, this::onPatchScoreChanged);
        addEventListener(FilesDroppedEvent.class, this::onFilesDropped);
        cfg.getSelectedPatches()
                .forEach(selectedPatch ->
                {
                    initDeviceStateFromConfig(selectedPatch.getDeviceStateId(), selectedPatch);
                    refreshCurrentDeviceState(selectedPatch.getDeviceStateId());
                    sendPatchToDevice(selectedPatch);
                });
        onDeviceChanged(null);
        midiRouter.setControllerMessageListener(this::onMidiController);
        midiRouter.listenDawOutputs();
    }

    private void onMidiController(String s) {
        Platform.runLater(() -> {
            getModel().setEventInfo(s);
        });
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
                refreshModeProperties(model, device, model.getCurrentDeviceState());
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
        String widgetId = selectionChangedEvent.getWidgetId();
        log.info(widgetId + " changed ! " + selectionChangedEvent.getSelectedIndexes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_DEVICE)) {
            onDeviceChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_MODE)) {
            onModeChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_MODE_CHANNEL)) {
            onChannelChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_CATEGORY)) {
            onCategoriesChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_BANK)) {
            onBankChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_PATCH)) {
            onPatchChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_PASSTHRU_OUTPUTS)) {
            onPassThruChanged(selectionChangedEvent);
        } else if (widgetId.equals(WidgetIdentifiers.WIDGET_ID_MASTER_INPUTS)) {
            onMasterInputChanged(selectionChangedEvent);
        }
    }

    private void onChannelChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        Integer channel = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(obj -> (Integer) obj)
                .findFirst()
                .orElse(null);
        saveDeviceState();
        var state = model.getCurrentDeviceState();
        if (state != null) {
            var id = new DeviceStateId(state.getId()
                    .getName(), state.getId()
                    .getMode(), channel);
            if (!model.getDeviceStates()
                    .containsKey(id)) {
                initDeviceStateFromConfig(id, null);
            }
            refreshCurrentDeviceState(id);
        }
        refreshPatches();
    }

    private void onMasterInputChanged(SelectionChangedEvent selectionChangedEvent) {
        String deviceOrPortName = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(obj -> (String) obj)
                .findFirst()
                .orElse(null);
        midiRouter.changeMainSource(deviceOrPortName);
    }

    private void onPassThruChanged(SelectionChangedEvent selectionChangedEvent) {
        List<String> selectedItems = selectionChangedEvent.getSelectedItems()
                .stream()
                .map(obj -> (String) obj)
                .toList();
        midiRouter.changeDestinations(selectedItems);
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
                setCurrentPatch(patch);
            }
        }
    }

    private void sendPatchToDevice(Patch selectedPatch) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(selectedPatch.getDevice())
                .orElseThrow();
        log.info("Send patch '{}' to '{}' on channel {} via MIDI port '{}'", selectedPatch.getName(),
                selectedPatch.getDevice(), selectedPatch.getChannel(),
                device.getOutputMidiDevice());
        MidiOutDevice port = getModel().getCurrentDeviceState()
                .getMidiOutDevice();
        if (port == null) {
            log.info("Port not open: " + device.getOutputMidiDevice());
            return;
        }
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
            MidiPreset midiPreset = MidiPresetBuilder.parse(device.getDefinitionFile(), selectedPatch.getChannel(),
                    device.getPresetFormat(),
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
            int channel = selectedPatch.getChannel();
            log.info("Select Edit Buffer on channel %d with preset format %s: %s".formatted(channel, device.getPresetFormat(), digits.stream()
                    .map(v -> "$%02X".formatted(v))
                    .collect(Collectors.joining(","))));
            switch (device.getPresetFormat()) {
                case NO_BANK_PRG -> {
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_MSB_PRG -> {
                    port.sendBankMSB(channel, digits.getFirst());
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_LSB_PRG -> {
                    port.sendBankLSB(channel, digits.getFirst());
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_MSB_LSB_PRG -> {
                    port.sendBankMSB(channel, digits.getFirst());
                    port.sendBankLSB(channel, digits.get(1));
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_PRG_PRG -> {
                    port.sendProgramChange(channel, digits.getFirst());
                    port.sendProgramChange(channel, digits.getLast());
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

    /**
     * Called when the user select a patch. Update the device accordingly via MIDI
     *
     * @param selectedPatch
     */
    private void setCurrentPatch(Patch selectedPatch) {
        getModel().getCurrentDeviceState()
                .setCurrentPatch(selectedPatch);
        saveSelectedPatchToConfig(selectedPatch);
        sendPatchToDevice(selectedPatch);
        saveDeviceState();
    }

    /**
     * Save the user selection to restore it when the application start
     */
    private void saveSelectedPatchToConfig(Patch selectedPatch) {
        var list = cfg.getSelectedPatches()
                .stream()
                .filter(sp -> !sp.getDeviceStateId()
                        .equals(selectedPatch.getDeviceStateId()))
                .collect(Collectors.toList());
        list.add(selectedPatch);
        cfg.setSelectedPatches(list);
        configurationFactory.saveConfig(cfg);
    }

    /**
     * Update the view regarding the list of patches matching the current filters
     * <p>This operation is not trivial, it is not just a search, because we
     * need to merge 2 patches lists in one:</p>
     * <ul>
     *     <li>The list of patches from the current device mode. Those don't have a score</li>
     *     <li>The list of favorite patches from the user, with a given score on each of them</li>
     * </ul>
     * <p>This method also update the info bar on the bottom of the UI</p>
     */
    private void refreshPatches() {
        var model = getModel();
        if (model.getCurrentDeviceState()
                .getId()
                .getName() == null)
            return;
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceState()
                        .getId()
                        .getName())
                .orElseThrow();
        List<Patch> patches = List.of();

        String currentModeName = model.getCurrentDeviceState()
                .getId()
                .getMode();
        if (currentModeName != null) {
            // Note: it is possible that currentModeBankName is not yet updated at this point
            // so midiDeviceMode will be null because it points to the previous selected device
            String currentModeBankName = model.getCurrentDeviceState()
                    .getCurrentBank();
            MidiDeviceMode midiDeviceMode = device.getDeviceModes()
                    .get(currentModeName);
            if (midiDeviceMode != null) {
                int channel = model.getCurrentDeviceState()
                        .getId()
                        .getChannel();
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
                                        configurationFactory.getFavorite(forgePatch(device.getDeviceName(), currentModeName, channel, bank.getName(), preset)))
                                .filter(this::patchScoreMatches))
                        .sorted(Comparator.comparing(Patch::getName))
                        .toList();
            }
        }
        // Update the info bar
        model.setInfo("%s | MIDI OUT '%s' | %d patches".formatted(device.getDeviceName(), device.getOutputMidiDevice(), patches.size()));
        // store the search output in the current observable state
        DeviceState deviceState = model.getCurrentDeviceState();
        deviceState
                .setCurrentSearchOutput(patches);
        saveDeviceState();
    }

    private Patch forgePatch(String deviceName, String currentModeName, int channel, String bankName, MidiDevicePreset preset) {
        return new Patch(deviceName, currentModeName, bankName, preset.name(), preset.category(), preset.command(), preset.filename(), channel, 0);
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

    /**
     * Called when the used change device mode
     */
    private void onModeChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        if (model.getCurrentDeviceState()
                .getId()
                .getName() == null) {
            refreshPatches();
            return;
        }
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceState()
                        .getId()
                        .getName())
                .orElseThrow();
        var state = model.getCurrentDeviceState();
        if (!selectionChangedEvent.getSelectedItems()
                .isEmpty()) {
            String modeName = (String) selectionChangedEvent.getSelectedItems()
                    .getFirst();
            log.info("Current Mode: " + modeName);
            changeModeOnDevice(device, state, modeName);
            refreshModeProperties(model, device, state);
            state.setCurrentSelectedCategories(List.of());
            state.setCurrentBank(null);
            state.setCurrentSearchOutput(null);
        } else {
            log.info("No mode selected, emptying everything...");
            model.setModeCategories(List.of());
            model.setModeChannels(List.of());
            model.setModeBanks(List.of());
            state.setCurrentBank(null);
            state.setCurrentSearchOutput(null);
        }
        refreshPatches();
    }

    /**
     * Update the view regarding various lists of items related to the selected device mode
     * <ul>
     *     <li>Mode banks</li>
     *     <li>Mode categories</li>
     *     <li>Mode channels</li>
     * </ul>
     */
    private void refreshModeProperties(MainModel model, MidiDeviceDefinition device, DeviceState state) {
        log.info("---------------------------------------------------------------------------------");

        var mode = device.getDeviceModes()
                .get(state.getId()
                        .getMode());
        if (mode != null) {
            log.info("Set categories from mode " + mode.getName());
            model.setModeCategories(mode.getCategories()
                    .stream()
                    .sorted(Comparator.comparing(MidiPresetCategory::name))
                    .toList());
            log.info("Set channels from mode " + mode.getName());
            model.setModeChannels(mode.getChannels());
            log.info("Set banks from mode " + mode.getName());
            model.setModeBanks(mode.getBanks()
                    .values()
                    .stream()
                    .sorted((b1, b2) -> {
                        if (b1.getCommand() == null && b2.getCommand() != null) {
                            return 1;
                        } else if (b2.getCommand() == null && b1.getCommand() != null) {
                            return -1;
                        } else {
                            return b1.getName()
                                    .compareTo(b2.getName());
                        }
                    })
                    .map(MidiDeviceBank::getName)
                    .toList());
        } else {
            log.warn("No mode selected, emptying everything...");
            model.setModeCategories(List.of());
            model.setModeBanks(List.of());
            model.setModeChannels(List.of());
        }
    }

    /**
     * If the mode is changed, change effectively the mode in the hardware device via MIDI
     */
    public void changeModeOnDevice(MidiDeviceDefinition device, DeviceState currentState, String newModeName) {
        if (!currentState.getId()
                .getMode()
                .equals(newModeName)) {
            String modeCommand = device.getDeviceModes()
                    .get(newModeName)
                    .getCommand();
            MidiOutDevice midiOutDevice = currentState.getMidiOutDevice();
            if (modeCommand != null && midiOutDevice != null) {
                log.info("Switch to mode: " + newModeName);
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
            }
        } else {
            log.info("Already in mode: " + newModeName);
        }

    }

    /**
     * Update the view given a selected device and select this device as the main destination for the MIDI router
     */
    private void onDeviceChanged(SelectionChangedEvent selectionChangedEvent) {
        var model = getModel();
        if (selectionChangedEvent == null || selectionChangedEvent.getSelectedIndexes()
                .isEmpty()) {
            model.setCurrentDeviceState(null);
            model.setModeBanks(List.of());
            model.setModeCategories(List.of());
            model.setDeviceModes(List.of());
            model.setModeChannels(List.of());
        } else {
            String deviceName = model
                    .getDevices()
                    .get(selectionChangedEvent.getSelectedIndexes()
                            .getFirst());
            var device = cfg.getMidiDeviceLibrary()
                    .getDevice(deviceName)
                    .orElseThrow();
            midiRouter.changeMainDestination(device);
            var states = model.getDeviceStates()
                    .values()
                    .stream()
                    .filter(s -> s.getId()
                            .getName()
                            .equals(deviceName) && s.isLastUsed())
                    .toList();
            dumpStates();
            DeviceStateId id = null;
            if (states.isEmpty()) {
                id = device.getDeviceModes()
                        .values()
                        .stream()
                        .findFirst()
                        .map(MidiDeviceMode::getName)
                        .map(name -> new DeviceStateId(deviceName, name, 0))
                        .orElse(null);
                if (id == null) {
                    return;
                }
                initDeviceStateFromConfig(id, null);
            } else {
                id = states.getFirst()
                        .getId();
            }
            var modes = device.getDeviceModes()
                    .keySet()
                    .stream()
                    .sorted()
                    .toList();
            model.setDeviceModes(modes);
            refreshCurrentDeviceState(id);
            refreshPatches();
        }
    }

    /**
     * Log the content of the unobseravble states in the map {@link MainModel#getDeviceStates()}
     */
    private void dumpStates() {
        getModel().getDeviceStates()
                .values()
                .stream()
                .sorted(Comparator.comparing((DeviceState s) -> s.getId()
                                .getName())
                        .thenComparingInt(s -> s.getId()
                                .getChannel()))

                .forEach(
                        (source) -> log.info("         {} lastUsed: '{}' command '{}' categories '{}' patch '{}'",
                                source.getId(),
                                source.isLastUsed(),
                                source.getCurrentBank(),
                                source.getCurrentSelectedCategories(),
                                source.getCurrentPatch()
                        ));
    }

    /**
     * put back the current state to the map {@link MainModel#getDeviceStates()}
     * <p>Note that the state can be an observable one now</p>
     */
    private void saveDeviceState() {
        var model = getModel();
        var current = model.getCurrentDeviceState();
        if (current.getId()
                .getName() != null) {
            log.info("---------------------------------------------------------------------------------");
            log.info("Save {} state: mode '{}' channel: '{}' selected bank '{}' selected categories '{}' selected patch '{}'",
                    current.getId()
                            .getName(),
                    current.getId()
                            .getMode(),
                    current.getId()
                            .getChannel(),
                    current.getCurrentBank(),
                    current.getCurrentSelectedCategories(),
                    current.getCurrentPatch()
            );
            var target = model.getDeviceStates()
                    .get(current.getId());
            if (!target.getId()
                    .getName()
                    .equals(current.getId()
                            .getName())) {
                throw new IllegalStateException();
            }
            midiRouter.changeOutputChannel(current.getId()
                    .getChannel());
            model.getDeviceStates()
                    .put(current.getId(), current);
        } else {
            log.info("Nothing to save, no device selected");
        }
        //dumpStates();
    }

    /**
     * Restore the UI with a given device state
     *
     * @param model    the current state displayed on screen
     * @param newState a state from the map {@link MainModel#getDeviceStates()}
     */
    private void refreshCurrentDeviceState(MainModel model, DeviceState newState) {
        log.info("---------------------------------------------------------------------------------");
        log.info("Switch to {} command '{}' categories '{}' patch '{}'", newState.getId(), newState.getCurrentBank(), newState.getCurrentSelectedCategories(), newState.getCurrentPatch());
        updateLastUsed(model, newState);
        model.setCurrentDeviceState(newState);
    }

    private void updateLastUsed(MainModel model, DeviceState newState) {
        model.getDeviceStates()
                .values()
                .stream()
                .filter(s -> s.getId()
                        .getName()
                        .equals(newState.getId()
                                .getName()))
                .forEach(s -> s.setLastUsed(false));
        newState.setLastUsed(true);
    }

    /**
     * Create a device state in the map {@link MainModel#getDeviceStates()} if needed
     * <p>This does NOT update the UI</p>
     *
     * @param id            state key used to store it in the map
     * @param selectedPatch optional, can be null
     */
    private void initDeviceStateFromConfig(DeviceStateId id, Patch selectedPatch) {

        var device = cfg.getMidiDeviceLibrary()
                .getDevice(id.getName())
                .orElseThrow();
        DeviceState deviceState;
        deviceState = new DeviceState();
        deviceState.setId(new DeviceStateId(id.getName(), id.getMode(), selectedPatch == null ? id.getChannel() : selectedPatch.getChannel()));
        if (deviceState.getMidiOutDevice() == null & device.getOutputMidiDevice() != null) {
            try {
                deviceState.setMidiOutDevice(cfg.getMidiDeviceManager()
                        .openOutput(device.getOutputMidiDevice()));
            } catch (MidiError e) {
                log.error("Unable to open device " + device.getOutputMidiDevice(), e);
            }
        }
        if (selectedPatch != null) {
            deviceState.setCurrentPatch(selectedPatch);
        }
        getModel().getDeviceStates()
                .put(deviceState.getId(), deviceState);
    }

    /**
     * Restore the current observable state from one in the map {@link MainModel#getDeviceStates()}
     *
     * @param id key of the state in the map
     */
    private void refreshCurrentDeviceState(DeviceStateId id) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(id.getName())
                .orElseThrow();
        var model = getModel();
        DeviceState deviceState = model.getDeviceStates()
                .get(id);

        // update the view with it
        refreshModeProperties(model, device, deviceState);
        refreshCurrentDeviceState(model, deviceState);
    }


}
