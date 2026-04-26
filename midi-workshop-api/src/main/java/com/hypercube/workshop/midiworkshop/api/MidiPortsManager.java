package com.hypercube.workshop.midiworkshop.api;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.AbstractMidiDevice;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.HardwareMidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MultiMidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.HardwareMidiOutPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.ports.remote.client.NetworkMidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.remote.client.NetworkMidiOutPort;
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
    private final List<MidiInPort> inputs = new ArrayList<>();
    /**
     * This list include real hardware output devices and network ones
     */
    private final List<MidiOutPort> outputs = new ArrayList<>();

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
                            outputs.add(new HardwareMidiOutPort(device));
                        }
                    }
                    if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
                        actualInputs.add(deviceName);
                        if (getInput(deviceName).isEmpty()) {
                            if (!firstScan) {
                                log.info("New input MIDI device detected: {}", deviceName);
                            }
                            inputs.add(new HardwareMidiInPort(device));
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

    public Optional<MidiInPort> getInput(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getInputs().stream()
                .filter(d -> d.getName()
                        .equals(name))
                .findFirst()
                .orElseGet(() -> {
                    if (NetworkMidiOutPort.isRemoteAddress(name)) {
                        return getOutput(name).map(o -> {
                                    NetworkMidiOutPort networkMidiOutPort = (NetworkMidiOutPort) o;
                                    if (!networkMidiOutPort.isOpen()) {
                                        networkMidiOutPort.open();
                                    }
                                    var midiInNetworkDevice = networkMidiOutPort.getNetworkMidiInPort();
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

    public Optional<MidiOutPort> getOutput(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getOutputs().stream()
                .filter(d -> d.getName()
                        .equals(name))
                .findFirst()
                .orElseGet(() -> {
                    if (NetworkMidiOutPort.isRemoteAddress(name)) {
                        NetworkMidiOutPort device = new NetworkMidiOutPort(name);
                        outputs.add(device);
                        return device;
                    } else {
                        return null;
                    }
                }));
    }

    public MidiOutPort openOutput(String name) {
        MidiOutPort out = getOutput(name).orElseThrow(() -> new MidiError("Output Port '%s' not found".formatted(name)));
        out.open();
        return out;
    }

    public MidiInPort openInput(String name) {
        MidiInPort in = getInput(name).orElseThrow(() -> new MidiError("Input Port '%s' not found".formatted(name)));
        in.open();
        return in;
    }

    public MidiInPort openInputs(List<MidiInPort> devices) {
        MidiInPort in = new MultiMidiInPort(devices);
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
                .filter(d -> !(d instanceof NetworkMidiInPort))
                .toList();
        var outputsToDelete = outputs.stream()
                .filter(d -> !actualHardwareOutputs.contains(d.getName()))
                .filter(d -> !(d instanceof NetworkMidiOutPort))
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
