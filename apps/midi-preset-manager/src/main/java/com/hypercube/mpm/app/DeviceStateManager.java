package com.hypercube.mpm.app;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialog;
import com.hypercube.mpm.javafx.widgets.dialog.progress.ProgressDialogController;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.DeviceStateId;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.DialogIcon;
import com.hypercube.util.javafx.worker.LongWork;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.thread.CancelNotifier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This class takes care of the device states displayed in the GUI
 * <ul>
 *     <li>When the user select another device, the "gui state" of the current one need to be saved</li>
 *     <li>When the user select the previous one, the "gui state" is restored</li>
 *     <li>The class {@link DeviceStateId} take a central role in this process</li>
 *     <ul>
 *         <li>it depicts what the user has selected: a device, a mode and a channel</li>
 *         <li>Multichannel devices will have 16 {@link DeviceState} in memory if the user select all of them</li>
 *         <li>When the user select for the very fist time a {@link DeviceStateId} we create it</li>
 *     </ul>
 * </ul>
 * <p>Under the hood all states are saved in the map {@link MainModel#getDeviceStates()} which is not observable</p>
 * <p>The key for this map is obviously a {@link DeviceStateId}</p>
 */
@Service
@Slf4j
public class DeviceStateManager {
    private final MainModel model;
    private final ConfigurationFactory configurationFactory;
    private final MidiDeviceRequester midiDeviceRequester;
    private final PatchesManager patchesManager;

    public DeviceStateManager(ConfigurationFactory configurationFactory, MidiDeviceRequester midiDeviceRequester, PatchesManager patchesManager) {
        this.configurationFactory = configurationFactory;
        this.midiDeviceRequester = midiDeviceRequester;
        this.patchesManager = patchesManager;
        this.model = MainModel.getObservableInstance();
    }

    public void initDeviceStateWithPatch(Patch selectedPatch) {
        initDeviceStateFromConfig(selectedPatch.getDeviceStateId(), selectedPatch);
        refreshCurrentDeviceState(selectedPatch.getDeviceStateId());
        changeModeOnDevice(getSelectedDevice(), model.getCurrentDeviceState(), selectedPatch.getMode(), true);
    }

    public void onModeChanged(String selectedMode) {
        if (model.getCurrentDeviceState() == null || selectedMode == null || selectedMode.isEmpty()) {
            return;
        }
        var device = getSelectedDevice();
        var currentState = model.getCurrentDeviceState();
        log.info("Switch from mode '{}' to mode '{}'", currentState.getId()
                .getMode(), selectedMode);
        changeModeOnDevice(device, currentState, selectedMode, false);
        var stateId = getOrCreateDeviceStateId(device, selectedMode);
        currentState = model.getDeviceStates()
                .get(stateId);
        updateLastUsed(model, currentState);
        model.setCurrentDeviceState(currentState);
        refreshModeProperties(device, currentState);
        currentState.setCurrentSelectedCategories(new ArrayList<>());
        currentState.setSelectedBankNames(new ArrayList<>());
        currentState.setCurrentSearchOutput(null);
    }

    /**
     * Create a device state in the map {@link MainModel#getDeviceStates()} if needed
     * <p>This does NOT update the UI</p>
     *
     * @param id            state key used to store it in the map
     * @param selectedPatch optional, can be null
     */
    public void initDeviceStateFromConfig(DeviceStateId id, Patch selectedPatch) {
        var cfg = configurationFactory.getProjectConfiguration();
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(id.getName())
                .orElseThrow();
        DeviceState deviceState;
        deviceState = new DeviceState();
        deviceState.setId(new DeviceStateId(id.getName(), id.getMode(), selectedPatch == null ? id.getChannel() : selectedPatch.getChannel()));
        if (deviceState.getMidiOutDevice() == null && device.getOutputMidiDevice() != null) {
            try {
                deviceState.setMidiOutDevice(cfg.getMidiPortsManager()
                        .openOutput(device.getOutputMidiDevice()));
            } catch (MidiError e) {
                log.error("Unable to open device {}", device.getOutputMidiDevice(), e);
            }
        }
        if (selectedPatch != null) {
            deviceState.setCurrentPatch(selectedPatch);
        }
        model.getDeviceStates()
                .put(deviceState.getId(), deviceState);
    }

    public void onChannelChanged(MainModel model, Integer channel) {
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
            saveDeviceState();
        }
    }

    /**
     * put back the current state to the map {@link MainModel#getDeviceStates()}
     * <p>Note that the state can be an observable one now</p>
     */
    public void saveDeviceState() {
        var current = model.getCurrentDeviceState();
        if (current == null) {
            return;
        }
        if (current.getId()
                .getName() != null) {
            log.info("##############################################");
            log.info("Save {} state: lastUsed: '{}' mode '{}' channel: '{}' selected bank '{}' selected categories '{}' selected patch '{}'",
                    current.getId()
                            .getName(),
                    current.isLastUsed(),
                    current.getId()
                            .getMode(),
                    current.getId()
                            .getChannel(),
                    current.getSelectedBankNames(),
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
            model.getDeviceStates()
                    .put(current.getId(), current);
        } else {
            log.info("Nothing to save, no device selected");
        }
        dumpStates();
    }

    /**
     * Update the view given a selected device
     */
    public void onDeviceChanged(MidiDeviceDefinition device) {
        if (device == null) {
            model.setCurrentDeviceState(null);
            model.setModeBanks(List.of());
            model.setModeCategories(List.of());
            model.setDeviceModes(List.of());
            model.setModeChannels(List.of());
        } else {
            dumpStates();
            final DeviceStateId id = getLastUsedDeviceStateId(device);
            refreshDeviceModes(device);
            refreshCurrentDeviceState(id);
        }
    }

    /**
     * If the mode is changed, change effectively the mode in the hardware device via MIDI
     */
    public void changeModeOnDevice(MidiDeviceDefinition device, DeviceState currentState, String newModeName, boolean force) {
        var cfg = configurationFactory.getProjectConfiguration();
        if (!currentState.getId()
                .getMode()
                .equals(newModeName) || force) {
            MidiDeviceMode midiDeviceMode = device.getDeviceModes()
                    .get(newModeName);
            if (midiDeviceMode != null) {
                Optional.ofNullable(midiDeviceMode.getCommand())
                        .ifPresent(modeCommand -> Optional.ofNullable(currentState.getMidiOutDevice())
                                .ifPresent(midiOutDevice -> {
                                    if (!newModeName.equals(getCurrentDeviceMode(device))) {
                                        log.info("Switch to Mode on device: {}", newModeName);
                                        model.getCurrentDeviceMode()
                                                .put(device, midiDeviceMode);
                                        midiDeviceRequester.updateDevice(device, null, midiOutDevice, CommandCall.parse(device.getDefinitionFile(), device, modeCommand));
                                        midiOutDevice.sleep(device.getModeLoadTimeMs());
                                    }
                                }));
            } else {
                log.error("Unknown mode '{}' for device '{}'", newModeName, device.getDeviceName());
            }
        } else {
            log.info("Already in mode: {}", newModeName);
        }

    }

    /**
     * Update the view regarding various lists of items related to the selected device mode
     * <ul>
     *     <li>Mode banks</li>
     *     <li>Mode categories</li>
     *     <li>Mode channels</li>
     * </ul>
     */
    public void refreshModeProperties(MidiDeviceDefinition device, DeviceState state) {
        log.info("---------------------------------------------------------------------------------");
        if (!state.getId()
                .getName()
                .equals(device.getDeviceName())) {
            log.error("Illegal State device ! It is %s instead of %s".formatted(state.getId()
                    .getName(), device.getDeviceName()));
            return;
        }
        log.info("Update mode properties: {}", state.getId()
                .getMode());
        MidiDeviceMode mode = device.getDeviceModes()
                .get(state.getId()
                        .getMode());
        if (mode != null) {
            Integer channel = state.getId()
                    .getChannel();
            List<MidiPresetCategory> categories = getModeCategories(mode, channel);
            log.info("Set {} categories from mode {}", categories.size(), mode.getName());
            model.setModeCategories(categories);
            List<Integer> modeChannels = mode.getAllChannels();
            log.info("Set {} channels from mode {}", modeChannels.size(), mode.getName());
            model.setModeChannels(modeChannels);
            List<String> banks = mode.getBanksForChannel(channel)
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
                    .toList();
            log.info("Set {} banks from mode {} on channel {}", banks.size(), mode.getName(), channel);
            model.setModeBanks(banks);
        } else {
            log.warn("No mode selected, emptying everything...");
            model.setModeCategories(List.of());
            model.setModeBanks(List.of());
            model.setModeChannels(List.of());
        }
    }

    public void reloadMidiDeviceLibrary() {
        configurationFactory.forceLoadMidiDeviceLibrary();
        initModel();
    }

    @PostConstruct
    public void initModel() {
        model.setDevices(buildDeviceList());
        model.setMidiInPorts(buildMidiInPortsList());
        model.setMidiThruPorts(buildMidiThruPortsList());
    }

    public int getCurrentOutputChannel() {
        return model.getCurrentDeviceState()
                .getId()
                .getChannel();
    }

    /**
     * Restore the state of all devices when the application start
     */
    public void initDevices() {
        var cfg = configurationFactory.getProjectConfiguration();
        List<String> devices = model.getDevices();
        log.info("Midi Device Library active: {}", devices.size());
        if (devices
                .isEmpty()) {
            GenericDialogController.info("First Launch", """
                    This is the first time you run this application.
                    There is no device enabled yet in your library.
                    You need to assign MIDI Ports to devices you want to use.
                    Then they will appear in the list.
                    """);
        } else if (!cfg.getSelectedPatches()
                .isEmpty()) {
            CancelNotifier cancelNotifier = new CancelNotifier();
            ProgressDialogController dlg = DialogController.buildDialog(ProgressDialog.class, JavaFXApplication.getMainStage(), DialogIcon.NONE, true);
            dlg.updateTextHeader("Restore %d device states...".formatted(cfg.getSelectedPatches()
                    .size()));
            dlg.setCancelNotifier(cancelNotifier);
            LongWork longWork = new LongWork("initDevices", () -> {
                try {
                    var sp = cfg.getSelectedPatches();
                    int totalSteps = sp.size() * 2 + 1;
                    dlg.updateProgress(0, "Wake up MIDI out devices with ActiveSensing...");
                    AtomicInteger stepsCount = new AtomicInteger();
                    sp.stream()
                            .parallel()
                            .forEach(patch -> {
                                String deviceName = patch.getDevice();
                                executeOnDeviceOutput(cfg, deviceName, out -> {
                                    log.info("Wake up device '{}' on MIDI port '{}'", deviceName, out.getName());
                                    out.sleep(4000);
                                });
                                double currentTotal = (double) stepsCount.incrementAndGet() / totalSteps;
                                dlg.updateProgress(currentTotal);
                            });
                    for (Patch patch : sp) {
                        cancelNotifier.checkIfShouldStop();
                        double progress = (double) stepsCount.incrementAndGet() / totalSteps;
                        dlg.updateProgress(progress, "'%s' on '%s' ...".formatted(patch.getName(), patch.getDevice()));
                        initDeviceStateWithPatch(patch);
                        patchesManager.sendPatchToDevice(patch);
                    }
                } catch (CancellationException e) {
                    log.warn("Cancelled by user");
                }
                dlg.updateProgress(1, "Done");
                dlg.sleep(3000);
            });
            dlg.runLongTaskWithDialog(dlg, longWork);
        }
    }

    private void executeOnDeviceOutput(ProjectConfiguration cfg, String deviceName, Consumer<MidiOutDevice> fct) {
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(deviceName)
                .orElseThrow();
        cfg.getMidiPortsManager()
                .getOutput(device.getOutputMidiDevice())
                .ifPresent(out -> {
                    try (out) {
                        out.open();
                        fct.accept(out);
                    } catch (MidiError e) {
                        log.error("Unexpected error wakening device {}", device.getDeviceName(), e);
                    }
                });
    }

    private List<MidiPresetCategory> getModeCategories(MidiDeviceMode mode, int channel) {

        if (mode.getSubBanks() != null && mode.getSubBanks()
                .getChannels()
                .contains(channel)) {
            mode = mode.getSubBanks()
                    .getMode();
        }
        return mode.getCategories()
                .stream()
                .sorted(Comparator.comparing(MidiPresetCategory::name))
                .toList();
    }

    private MidiDeviceDefinition getSelectedDevice() {
        return configurationFactory.getProjectConfiguration()
                .getMidiDeviceLibrary()
                .getDevice(model.getCurrentDeviceState()
                        .getId()
                        .getName())
                .orElseThrow();
    }

    /**
     * When possible we replace the MIDI port name by a known device
     */
    private List<String> buildMidiInPortsList() {
        var cfg = configurationFactory.getProjectConfiguration();
        return cfg.getMidiPortsManager()
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
        var cfg = configurationFactory.getProjectConfiguration();
        return cfg.getMidiPortsManager()
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
        var cfg = configurationFactory.getProjectConfiguration();
        return cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .filter(d -> {
                    try {
                        return d.getOutputMidiDevice() != null && !d.getOutputMidiDevice()
                                .isEmpty() && cfg.getMidiPortsManager()
                                .getOutput(d.getOutputMidiDevice())
                                .isPresent();
                    } catch (MidiError e) {
                        log.error("Unexpected error on device {}. Disabled.", d.getDeviceName(), e);
                        return false;
                    }
                })
                .map(MidiDeviceDefinition::getDeviceName)
                .sorted()
                .toList();
    }

    /**
     * Create a new device state id and its state
     * <p>This method is called in two cases</p>
     * <lu>
     * <li>When the user select a device mode for the first time</li>
     * <li>When the user select a device for the first time: in this case 'defaultModeName' is null</li>
     * </lu>
     * <p>Under the hood, {@link MainModel#getDeviceStates()} will be updated</p>
     */
    private DeviceStateId forgeDefaultDeviceStateId(MidiDeviceDefinition device, String defaultModeName) {
        String deviceName = device.getDeviceName();
        // id can be null if no modes are defined (DAW device typically)
        final DeviceStateId id = device.getDeviceModes()
                .values()
                .stream()
                .filter(mode -> defaultModeName == null || mode.getName()
                        .equals(defaultModeName))
                .findFirst()
                .map(MidiDeviceMode::getName)
                .map(modeName -> new DeviceStateId(deviceName, modeName, 0))
                .orElse(null);
        if (id != null) {
            initDeviceStateFromConfig(id, null);
        }
        return id;
    }

    /**
     * When the user select a device mode:
     * <ul>
     *     <li>It is the technical equivalent to select the state id "device,mode,channel 0"</li>
     *     <li>If this state is not yet in the map {@link MainModel#getDeviceStates()}, it is created and added</li>
     * </ul>
     */
    private DeviceStateId getOrCreateDeviceStateId(MidiDeviceDefinition device, String modeName) {
        var id = new DeviceStateId(device.getDeviceName(), modeName, 0);
        if (!model.getDeviceStates()
                .containsKey(id)) {
            return forgeDefaultDeviceStateId(device, modeName);
        } else {
            return id;
        }
    }

    /**
     * Restore the current observable state from one in the map {@link MainModel#getDeviceStates()}
     *
     * @param id key of the state in the map
     */
    private void refreshCurrentDeviceState(DeviceStateId id) {
        if (id == null) {
            // device without modes, so no id, we empty everything
            model.setModeCategories(List.of());
            model.setModeBanks(List.of());
            model.setModeChannels(List.of());
            model.setCurrentDeviceState(null);
        } else {
            var device = configurationFactory.getProjectConfiguration()
                    .getMidiDeviceLibrary()
                    .getDevice(id.getName())
                    .orElseThrow();

            DeviceState deviceState = model.getDeviceStates()
                    .get(id);

            // update the view with it
            refreshCurrentDeviceState(device, deviceState);
            refreshModeProperties(device, deviceState);
        }
    }

    /**
     * for a given device, we reset the previous "lastUsed" and set it to the new state
     */
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
        dumpStates();
    }

    /**
     * Log the content of the unobservable states in the map {@link MainModel#getDeviceStates()}
     */
    private void dumpStates() {
        model.getDeviceStates()
                .values()
                .stream()
                .sorted(Comparator.comparing((DeviceState s) -> s.getId()
                                .getName())
                        .thenComparingInt(s -> s.getId()
                                .getChannel()))

                .forEach(
                        (source) -> log.info("         {} lastUsed: '{}' command '{}' categories '{}' patch '{}'",
                                "%40s".formatted((source.isLastUsed() ? "->" : "  ") + source.getId()),
                                source.isLastUsed(),
                                source.getSelectedBankNames(),
                                source.getCurrentSelectedCategories(),
                                source.getCurrentPatch()
                        ));
    }

    /**
     * Restore the UI with a given device state
     *
     * @param device
     * @param newState a state from the map {@link MainModel#getDeviceStates()}
     */
    private void refreshCurrentDeviceState(MidiDeviceDefinition device, DeviceState newState) {
        log.info("---------------------------------------------------------------------------------");
        log.info("Switch to {} lastUsed '{}' command '{}' categories '{}' patch '{}'", newState.getId(),
                newState.isLastUsed(),
                newState.getSelectedBankNames(), newState.getCurrentSelectedCategories(), newState.getCurrentPatch());
        updateLastUsed(model, newState);
        boolean requireSetMode = model.getCurrentDeviceState() == null;
        model.setCurrentDeviceState(newState);
        changeModeOnDevice(device, newState, newState.getId()
                .getMode(), true);
    }

    private String getCurrentDeviceMode(MidiDeviceDefinition device) {
        return Optional.ofNullable(model.getCurrentDeviceMode()
                        .get(device))
                .map(MidiDeviceMode::getName)
                .orElse(null);
    }

    /**
     * Peek the latest used state id or create a new one if needed
     * <p>Typically if the user was working on channel 4, we try to get back to this state</p>
     *
     * @param device currently selected device
     */
    private DeviceStateId getLastUsedDeviceStateId(MidiDeviceDefinition device) {
        String deviceName = device.getDeviceName();
        var lastUsedState = model.getDeviceStates()
                .values()
                .stream()
                .filter(s -> s.getId()
                        .getName()
                        .equals(deviceName) && s.isLastUsed())
                .findFirst()
                .orElse(null);
        if (lastUsedState == null) {
            return forgeDefaultDeviceStateId(device, null);
        } else {
            return lastUsedState.getId();
        }
    }

    /**
     * Note: it is perfectly possible to have no modes for a device (DAW device for instance)
     */
    private void refreshDeviceModes(MidiDeviceDefinition device) {
        var modes = device.getDeviceModes()
                .keySet()
                .stream()
                .sorted()
                .toList();
        model.setDeviceModes(modes);
    }
}
