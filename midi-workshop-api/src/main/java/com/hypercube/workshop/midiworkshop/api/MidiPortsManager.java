package com.hypercube.workshop.midiworkshop.api;

import com.hypercube.workshop.midiworkshop.api.devices.AbstractMidiDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MultiMidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.remote.client.MidiInNetworkDevice;
import com.hypercube.workshop.midiworkshop.api.devices.remote.client.MidiOutNetworkDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.*;

/**
 * Since this framework introduce Network Midi devices, Java API knows nothing about them
 * <p>Refreshing the list of devices is a little bit tricky because of that</p>
 */
@Slf4j
@Getter
@Service
public class MidiPortsManager {
    /**
     * This list include real hardware input devices and network ones
     */
    private final List<MidiInDevice> inputs = new ArrayList<>();
    /**
     * This list include real hardware output devices and network ones
     */
    private final List<MidiOutDevice> outputs = new ArrayList<>();

    public static boolean isJdkVersionAtLeast(int requiredMajorVersion) {
        String javaVersion = System.getProperty("java.version");

        // Before JDK 9
        if (javaVersion.startsWith("1.")) {
            try {
                int majorVersion = Integer.parseInt(javaVersion.substring(2, 3));
                return majorVersion >= requiredMajorVersion;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            // JDK9+
            try {
                String[] parts = javaVersion.split("\\.");
                int majorVersion = Integer.parseInt(parts[0]);
                return majorVersion >= requiredMajorVersion;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    /**
     * Refresh the list of devices given the list of hardware devices provided by Java API
     * <ul>
     *     <li>Network devices are untouched</li>
     *     <li>Hardware devices are removed or added</li>
     * </ul>
     * This method refresh {@link #inputs} and {@link #outputs}
     */
    public void collectHardwareDevices() {
        if (!isJdkVersionAtLeast(23)) {
            log.error("===================================================================================================");
            log.error("Your JDK is too old and contains serious MIDI bugs, please upgrade to 23+. Current version is: " + System.getProperty("java.version"));
            log.error("===================================================================================================");
        }
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        Set<String> actualInputs = new HashSet<>();
        Set<String> actualOutputs = new HashSet<>();
        boolean firstScan = inputs.isEmpty() && outputs.isEmpty();
        for (MidiDevice.Info info : devices) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (!(device instanceof Sequencer) && !(device instanceof Synthesizer)) {
                    String deviceName = info.getName();
                    if (device.getMaxReceivers() > 0 || device.getMaxReceivers() == -1) {
                        actualOutputs.add(deviceName);
                        if (getOutput(deviceName).isEmpty()) {
                            if (!firstScan) {
                                log.info("New output MIDI device detected: {}", deviceName);
                            }
                            outputs.add(new MidiOutDevice(device));
                        }
                    }
                    if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
                        actualInputs.add(deviceName);
                        if (getInput(deviceName).isEmpty()) {
                            if (!firstScan) {
                                log.info("New input MIDI device detected: {}", deviceName);
                            }
                            inputs.add(new MidiInDevice(device));
                        }
                    }
                }

            } catch (MidiUnavailableException e) {
                log.error("Port {} is not available", info.getDescription());
            }
        }
        cleanupHardwareDevices(actualInputs, actualOutputs);
        sortDevices();
    }

    public Optional<MidiInDevice> getInput(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getInputs().stream()
                .filter(d -> d.getName()
                        .equals(name))
                .findFirst()
                .orElseGet(() -> {
                    if (MidiOutNetworkDevice.isRemoteAddress(name)) {
                        return getOutput(name).map(o -> {
                                    MidiOutNetworkDevice midiOutNetworkDevice = (MidiOutNetworkDevice) o;
                                    var midiInNetworkDevice = midiOutNetworkDevice.getMidiInNetworkDevice();
                                    if (midiInNetworkDevice != null && !inputs.contains(midiInNetworkDevice)) {
                                        inputs.add(midiInNetworkDevice);
                                    }
                                    return midiInNetworkDevice;
                                })
                                .orElse(null);
                    } else {
                        return null;
                    }
                }));
    }

    public Optional<MidiOutDevice> getOutput(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getOutputs().stream()
                .filter(d -> d.getName()
                        .equals(name))
                .findFirst()
                .orElseGet(() -> {
                    if (MidiOutNetworkDevice.isRemoteAddress(name)) {
                        MidiOutDevice device = new MidiOutNetworkDevice(name);
                        outputs.add(device);
                        return device;
                    } else {
                        return null;
                    }
                }));
    }

    public MidiOutDevice openOutput(String name) {
        MidiOutDevice out = getOutput(name)
                .orElseThrow(() -> new MidiError("Output Port '%s' not found".formatted(name)));
        out.open();
        return out;
    }

    public MidiInDevice openInput(String name) {
        MidiInDevice in = getInput(name)
                .orElseThrow(() -> new MidiError("Input Port '%s' not found".formatted(name)));
        in.open();
        return in;
    }

    public MidiInDevice openInputs(List<MidiInDevice> devices) {
        MidiInDevice in = new MultiMidiInDevice(devices);
        in.open();
        return in;
    }

    public void listPorts() {
        // List devices for convenience
        log.info("Available MIDI ports:");
        outputs
                .forEach(o -> log.info("OUT:" + o.getName()));
        inputs
                .forEach(o -> log.info("IN :" + o.getName()));

    }

    private void sortDevices() {
        inputs.sort(Comparator.comparing(AbstractMidiDevice::getName));
        outputs.sort(Comparator.comparing(AbstractMidiDevice::getName));
    }

    private void listDevices() {
        inputs.forEach(d -> log.info("INPUT  device {}", d.getName()));
        outputs.forEach(d -> log.info("OUTPUT device {}", d.getName()));
    }

    /**
     * If a hardware device is no longer listed by Java API, we close and remove it
     * <p>network devices are untouched since they are not listed by Java API, they are only part of this framework</p>
     */
    private void cleanupHardwareDevices(Set<String> actualHardwareInputs, Set<String> actualHardwareOutputs) {
        var inputsToDelete = inputs.stream()
                .filter(d -> !actualHardwareInputs.contains(d.getName()))
                .filter(d -> !(d instanceof MidiInNetworkDevice))
                .toList();
        var outputsToDelete = outputs.stream()
                .filter(d -> !actualHardwareOutputs.contains(d.getName()))
                .filter(d -> !(d instanceof MidiOutNetworkDevice))
                .toList();
        inputsToDelete.forEach(midiInDevice -> {
            log.info("Input MIDI device removed: {}", midiInDevice.getName());
            midiInDevice.close();
        });
        outputsToDelete.forEach(midiOutDevice -> {
            log.info("Output MIDI device removed: {}", midiOutDevice.getName());
            midiOutDevice.close();
        });
        inputs.removeAll(inputsToDelete);
        outputs.removeAll(outputsToDelete);
    }
}
