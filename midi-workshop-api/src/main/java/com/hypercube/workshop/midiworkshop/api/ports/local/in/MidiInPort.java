package com.hypercube.workshop.midiworkshop.api.ports.local.in;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.listener.SysExMidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.AbstractMidiDevice;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * As soon as the MidiInDevice is opened, it will listen messages with {@link Transmitter#setReceiver(Receiver)}
 * <ul>
 *     <li>This is hidden in the implementation. </li>
 *     <li>From the outside, you just have to call {@link #addListener(MidiListener)} and {@link #removeListener(MidiListener)}</li>
 *     <li>the listeners list is threadsafe, so you can add or remove listener from multiple threads</li>
 * </ul>
 */
@Slf4j
public abstract class MidiInPort extends AbstractMidiDevice {
    protected final Set<MidiListener> listeners = ConcurrentHashMap.newKeySet();
    @Getter
    private boolean isListening;
    private CountDownLatch listenTerminated;

    public MidiInPort(String name) {
        super(name);
    }

    public int getListenersCount() {
        return listeners.size();
    }

    public List<MidiListener> getListeners() {
        return listeners.stream()
                .toList();
    }

    /**
     * Blocking listen
     * <p>WARNING: Don't use this inside a try-with-resource, you will wait forever</p>
     * <p>
     * BEWARE, In JAVA, two method references pointing to the same method are not equals
     * <p>So make sure you pass the same each time otherwise the underling set {@link #listeners} won't do its job</p>
     */
    public void listen(MidiListener listener) {
        addListener(listener);
        waitNotListening();
    }

    /**
     * BEWARE, In JAVA, two method references pointing to the same method are not equals
     * <p>So make sure you pass the same each time otherwise the underling set {@link #listeners} won't do its job</p>
     */
    public void addListener(MidiListener listener) {
        listeners.add(listener);
    }

    /**
     * Add a listener wrapping it with a {@link SysExMidiListener} to receive ONLY SYSEX
     * <p>
     * BEWARE, In JAVA, two method references pointing to the same method are not equals
     * <p>So make sure you pass the same each time otherwise the underling set {@link #listeners} won't do its job</p>
     */
    public void addSysExListener(MidiListener listener) {
        addListener(new SysExMidiListener(listener));
    }

    /**
     * BEWARE, In JAVA, two method references pointing to the same method are not equals
     * <p>So make sure you pass the same each time otherwise the underling set {@link #listeners} won't do its job</p>
     */
    public void removeListener(MidiListener listener) {
        if (listener == null) {
            return;
        }
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        } else {
            // may be the listener is wrapped in a SysExMidiListener
            listeners.stream()
                    .filter(SysExMidiListener.class::isInstance)
                    .map(l -> (SysExMidiListener) l)
                    .filter(l -> l.getListener()
                            .equals(listener))
                    .findFirst()
                    .ifPresent(listeners::remove);
        }
    }

    /**
     * Make sure you don't call this method if you are in a try-with-resource, it will break the internal reference counter
     */
    @Override
    public void close() {
        if (getOpenCount() == 1) {
            stopListening();
        }
        super.close();
    }

    /**
     * Wait the device is closed, so it no longer listen
     * <p>WARNING: Don't use this inside a try-with-resource, you will wait forever</p>
     */
    public void waitNotListening() {
        try {
            if (listenTerminated != null) {
                listenTerminated.await();
            }
        } catch (InterruptedException e) {
            throw new MidiError(name, e);
        }
    }


    protected void stopListening() {
        log.info("Stop listening on device {}", getName());
        listeners.clear();
        isListening = false;
        if (listenTerminated != null) {
            listenTerminated.countDown();
        }
    }

    /**
     * Non-blocking listen. Does nothing if already listening
     */
    protected void startListening() {
        if (isListening) {
            return;
        }
        listenTerminated = new CountDownLatch(1);
        // Wait the Java MIDI API is ready to receive messages
        // There is no other way to do this better
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new MidiError(name, e);
        }
        isListening = true;
    }
}
