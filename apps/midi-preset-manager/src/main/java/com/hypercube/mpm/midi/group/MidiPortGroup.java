package com.hypercube.mpm.midi.group;

import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.AbstractMidiDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class MidiPortGroup<T extends AbstractMidiDevice> {
    private final ConfigurationService configurationService;
    private final String groupName;
    private final Consumer<T> openCallback;
    private final Consumer<MidiDeviceDefinition> closeCallback;

    /**
     * Used to protect the access of the list {@link #midiPorts} in a threadsafe way
     */
    private final Object portListGuardian = new Object();
    /**
     * MIDI ports
     */
    protected List<T> midiPorts = new ArrayList<>();
    @Getter
    private boolean muted;
    /**
     * List of ports given by the GUI (device names, or ports names)
     */
    private List<String> deviceOrPortNames = new ArrayList<>();

    public void changePorts(List<String> deviceOrPortNames) {
        closePorts();
        this.deviceOrPortNames = new ArrayList<>(deviceOrPortNames); // make it read/write
        openPorts();
    }

    public void mute(String device, boolean mute) {
        if (this.deviceOrPortNames.contains(device)) {
            muted = mute;
        }
    }

    public void forEach(Consumer<T> midiPortConsumer) {
        if (!muted) {
            synchronized (portListGuardian) {
                midiPorts.forEach(midiPortConsumer);
            }
        }
    }

    public void restart() {
        List<String> backupNames = new ArrayList<>(deviceOrPortNames);
        closePorts();
        deviceOrPortNames = backupNames;
        openPorts();
    }

    public void closePorts() {
        var cfg = getCfg();
        if (closeCallback != null) {
            getMidiDeviceDefinitions(cfg)
                    .forEach(closeCallback);
        }
        synchronized (portListGuardian) {
            for (T midiPort : midiPorts) {
                try {
                    log.info("Close {} port: '{}'", groupName, midiPort.getName());
                    midiPort.close();
                } catch (MidiError e) {
                    log.warn("Unable to close {} port: '{}'", groupName, midiPort.getName());
                }
            }
            midiPorts.clear();
        }
        deviceOrPortNames.clear();
    }

    protected abstract Optional<T> getPort(String name);

    protected abstract String getPort(MidiDeviceDefinition device);

    protected ProjectConfiguration getCfg() {
        return configurationService.getProjectConfiguration();
    }

    private void openPorts() {
        var cfg = getCfg();

        synchronized (portListGuardian) {
            midiPorts = resolvePortNames(cfg).stream()
                    .map(this::getPort)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            for (T midiPort : midiPorts) {
                if (openCallback != null) {
                    openCallback.accept(midiPort);
                }
                midiPort.open();
            }
        }
    }

    private List<String> resolvePortNames(ProjectConfiguration cfg) {
        return deviceOrPortNames.stream()
                .map(deviceOrPortName -> cfg.getMidiDeviceLibrary()
                        .getDevice(deviceOrPortName)
                        .map(this::getPort)
                        .orElse(deviceOrPortName))
                .toList();
    }

    private List<MidiDeviceDefinition> getMidiDeviceDefinitions(ProjectConfiguration cfg) {
        return deviceOrPortNames.stream()
                .map(deviceOrPortName -> cfg.getMidiDeviceLibrary()
                        .getDevice(deviceOrPortName))
                .flatMap(Optional::stream)
                .toList();
    }
}
