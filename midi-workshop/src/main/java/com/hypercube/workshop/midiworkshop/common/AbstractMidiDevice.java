package com.hypercube.workshop.midiworkshop.common;

import lombok.AllArgsConstructor;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

@AllArgsConstructor
public abstract class AbstractMidiDevice {
    protected final MidiDevice device;

    public void open() throws MidiUnavailableException {
        device.open();
    }
    public boolean isOpen()    {
        return device.isOpen();
    }
    public void close() {
        device.close();
    }
    public String getName() {
        return device.getDeviceInfo().getName();
    }

}
