package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.AllArgsConstructor;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

@AllArgsConstructor
public abstract class AbstractMidiDevice {
    protected final MidiDevice device;

    public void open() {
        try {
            device.open();
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
        }
    }

    public boolean isOpen() {
        return device.isOpen();
    }

    public void close() {
        device.close();
    }

    public String getName() {
        return device.getDeviceInfo()
                .getName();
    }

}
