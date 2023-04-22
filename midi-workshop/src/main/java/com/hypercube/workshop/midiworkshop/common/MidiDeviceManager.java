package com.hypercube.workshop.midiworkshop.common;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MidiDeviceManager {
    List<MidiInDevice> inputs = new ArrayList<>();
    List<MidiOutDevice> outputs = new ArrayList<>();
    public void collectDevices() {
        inputs.clear();
        outputs.clear();
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : devices) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (!(device instanceof Sequencer) && !(device instanceof Synthesizer)) {
                    if (device.getMaxReceivers() > 0 || device.getMaxReceivers()==-1) {
                        outputs.add(new MidiOutDevice(device));
                    }
                    if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters()==-1) {
                        inputs.add(new MidiInDevice(device));
                    }
                }

            } catch (MidiUnavailableException e) {
                System.out.println("Device " + info.getDescription() + " is not available");
            }
        }
    }

    public List<MidiInDevice> getInputs() {
        return inputs;
    }

    public List<MidiOutDevice> getOutputs() {
        return outputs;
    }

    public Optional<MidiInDevice> getInput(String name){
        return getInputs().stream().filter(d->d.getName().equals(name)).findFirst();
    }
    public Optional<MidiOutDevice> getOutput(String name){
        return getOutputs().stream().filter(d->d.getName().equals(name)).findFirst();
    }
}
