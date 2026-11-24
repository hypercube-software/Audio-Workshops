package com.hypercube.workshop.midiworkshop.api.ports;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiDevice;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Our Midi device is artificially multi-client using {@link #openCount}
 * <p>It means close does not really close until the counter is 1</p>
 * We also use synchronized on {@link #open()} and {@link #close()} to prevent any issue
 */
@Slf4j
public abstract class AbstractMidiDevice implements Closeable {
    @Getter
    protected final String name;
    private final AtomicInteger openCount = new AtomicInteger();

    protected AbstractMidiDevice(String name) {
        this.name = name;
    }

    /**
     * Most of the time {@link MidiDevice#open()} fail when the device is already open by another application
     */
    public synchronized int open() {
        int count = openCount.incrementAndGet();
        log.info("Open {} '{}' (client count is {})", getClass().getSimpleName(), getName(), count);
        if (count == 1) {
            try {
                effectiveOpen();
            } catch (Exception e) {
                openCount.decrementAndGet();
                throw new MidiError("Unable to open device " + getName(), e);
            }
        }
        return count;
    }

    @Override
    public synchronized void close() {
        String portType = this.getClass()
                .getSimpleName();
        int count = openCount.get();
        if (count == 0) {
            log.debug("{}::close on '{}' ignored: device already closed", portType, getName());
            return;
        }
        boolean open = isOpen();
        boolean effectiveClose = count == 1 && open;
        log.info("{}::close '{}' (client count is {}, isOpen: {}): {}", portType, getName(), count, open, effectiveClose ? "effective Close" : "keep open");
        if (effectiveClose) {
            try {
                effectiveClose();
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

    public abstract boolean isOpen();

    public String getUniqueName() {
        return this.getClass()
                .getSimpleName() + ":" + name;
    }

    protected abstract void effectiveOpen() throws Exception;

    protected abstract void effectiveClose();

}
