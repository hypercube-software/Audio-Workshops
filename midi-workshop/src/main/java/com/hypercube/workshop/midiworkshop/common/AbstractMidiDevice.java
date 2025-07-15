package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.AllArgsConstructor;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import java.io.Closeable;
import java.io.IOException;

@AllArgsConstructor
public abstract class AbstractMidiDevice implements Closeable {
    protected final MidiDevice device;

    public void open() {
        try {
            if (!device.isOpen()) {
                device.open();
            }
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (device.isOpen()) {
            device.close();
        }
    }

    public boolean isOpen() {
        return device.isOpen();
    }

    public String getName() {
        return device.getDeviceInfo()
                .getName();
    }

}
