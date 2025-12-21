package com.hypercube.workshop.midiworkshop.api.devices;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Our Midi device is artificially multi-client using {@link #openCount}
 * <p>It means close does not really close until the counter is 1</p>
 */
@AllArgsConstructor
@Slf4j
public abstract class AbstractMidiDevice implements Closeable {
    protected final MidiDevice device;
    protected final AtomicInteger openCount = new AtomicInteger();

    /**
     * Most of the time {@link MidiDevice#open()} fail when the device is already open by another application
     */
    public void open() {
        try {
            if (device != null && !device.isOpen()) {
                device.open();
            }
            int count = openCount.incrementAndGet();
            log.info("Open MIDI port '{}' (client count is {})", getName(), count);
        } catch (MidiUnavailableException e) {
            throw new MidiError(device, e);
        }
    }

    @Override
    public void close() {
        int count = openCount.get();
        log.info("Close MIDI port '{}' (client count is {})", getName(), count);
        if (count == 1 && device != null && device.isOpen()) {
            try {
                device.close();
            } catch (Exception e) {
                throw new MidiError(e);
            }
        }
        if (count >= 1) {
            openCount.decrementAndGet();
        }
    }

    public int getOpenCount() {
        return openCount.get();
    }

    public boolean isOpen() {
        return device != null && device.isOpen();
    }

    public String getName() {
        return device.getDeviceInfo()
                .getName();
    }

}
