package com.hypercube.workshop.midiworkshop.api.ports;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiDevice;
import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Our Midi device is artificially multi-client using {@link #openCount}
 * <p>It means close does not really close until the counter is 1</p>
 */
@Slf4j
public abstract class AbstractMidiDevice implements Closeable {
    private static final Set<String> instances = new HashSet<>();
    @Getter
    protected final String name;
    private final AtomicInteger openCount = new AtomicInteger();

    protected AbstractMidiDevice(String name) {
        this.name = name;
        sanityCheck();
    }

    /**
     * Most of the time {@link MidiDevice#open()} fail when the device is already open by another application
     */
    public void open() {
        int count = openCount.incrementAndGet();
        log.info("Open {} '{}' (client count is {})", getClass().getSimpleName(), getName(), count);
    }

    @Override
    public void close() {
        String portType = this.getClass()
                .getSimpleName();
        int count = openCount.get();
        if (count == 0) {
            throw new MidiError("%s::close on '%s' called too much, since count is 0, something is wrong in your code (use try-with-resource)".formatted(portType, getName()));
        }
        boolean open = isOpen();
        boolean effectiveClose = count == 1 && open;
        log.info("{}::close '{}' (client count is {}, isOpen: {}): {}", portType, getName(), count, open, effectiveClose ? "effective Close" : "keep open");
        if (effectiveClose) {
            try {
                effectiveClose();
                synchronized (instances) {
                    instances.remove(getUniqueName());
                }
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

    protected abstract void effectiveClose();

    private void sanityCheck() {
        String uniqueName = getUniqueName();
        synchronized (instances) {
            if (instances.contains(uniqueName)) {
                throw new IllegalStateException("Duplicate instance of midi device class '%s' detected".formatted(uniqueName));
            }
            instances.add(uniqueName);
        }
    }

}
