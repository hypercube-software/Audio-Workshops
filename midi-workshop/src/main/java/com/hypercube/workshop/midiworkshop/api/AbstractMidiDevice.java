package com.hypercube.workshop.midiworkshop.api;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.AllArgsConstructor;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public abstract class AbstractMidiDevice implements Closeable {
    protected final MidiDevice device;
    private final AtomicInteger openCount = new AtomicInteger();

    public void open() {
        try {
            openCount.incrementAndGet();
            if (!device.isOpen()) {
                device.open();
            }
        } catch (MidiUnavailableException e) {
            throw new MidiError(device, e);
        }
    }

    @Override
    public void close() throws IOException {
        int count = openCount.decrementAndGet();
        if (count == 0) {
            if (device.isOpen()) {
                device.close();
            }
        }
    }

    public int getOpenCount() {
        return openCount.get();
    }

    public boolean isOpen() {
        return device.isOpen();
    }

    public String getName() {
        return device.getDeviceInfo()
                .getName();
    }

}
