package com.hypercube.workshop.midiworkshop.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
public class MidiDeviceManager {
    private final List<MidiInDevice> inputs = new ArrayList<>();
    private final List<MidiOutDevice> outputs = new ArrayList<>();

    public void collectDevices() {
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
                log.error("Device " + info.getDescription() + " is not available");
            }
        }
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
}
