package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
public class MidiDeviceManager {
    private final List<MidiInDevice> inputs = new ArrayList<>();
    private final List<MidiOutDevice> outputs = new ArrayList<>();

    public void collectDevices() {
        if (!isJdkVersionAtLeast(23)) {
            log.error("===================================================================================================");
            log.error("Your JDK is too old and contains serious MIDI bugs, please upgrade to 23+. Current version is: " + System.getProperty("java.version"));
            log.error("===================================================================================================");
        }
        inputs.clear();
        outputs.clear();
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : devices) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (!(device instanceof Sequencer) && !(device instanceof Synthesizer)) {
                    if (device.getMaxReceivers() > 0 || device.getMaxReceivers() == -1) {
                        outputs.add(new MidiOutDevice(device));
                    }
                    if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
                        inputs.add(new MidiInDevice(device));
                    }
                }

            } catch (MidiUnavailableException e) {
                log.error("Port " + info.getDescription() + " is not available");
            }
        }
        inputs.sort(Comparator.comparing(AbstractMidiDevice::getName));
        outputs.sort(Comparator.comparing(AbstractMidiDevice::getName));
    }

    public Optional<MidiInDevice> getInput(String name) {
        return getInputs().stream()
                .filter(d -> d.getName()
                        .equals(name))
                .findFirst();
    }

    public Optional<MidiOutDevice> getOutput(String name) {
        return getOutputs().stream()
                .filter(d -> d.getName()
                        .equals(name))
                .findFirst();
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
}
