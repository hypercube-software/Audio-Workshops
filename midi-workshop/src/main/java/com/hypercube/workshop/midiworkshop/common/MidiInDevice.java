package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.common.listener.SysExMidiListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class MidiInDevice extends AbstractMidiDevice {
    private final Set<MidiListener> listeners = ConcurrentHashMap.newKeySet();
    private Transmitter transmitter;
    CountDownLatch listenTerminated;
    @Getter
    private boolean isListening;

    public MidiInDevice(MidiDevice device) {
        super(device);
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
     * <p>
     * BEWARE, In JAVA, two method references pointing to the same method are not equals
     * <p>So make sure you pass the same each time otherwise the underling set {@link #listeners} won't do its job</p>
     */
    public void listen(MidiListener listener) {
        addListener(listener);
        startListening();
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
     * Non-blocking listen. Does nothing if already listening
     */
    public void startListening() {
        if (isListening) {
            return;
        }
        listenTerminated = new CountDownLatch(1);
        closeTransmitter();

        try {
            transmitter = device.getTransmitter();
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
        }
        Receiver receiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                CustomMidiEvent event = new CustomMidiEvent(message, timeStamp);
                //log.info("%d listeners, receive %s".formatted(listeners.size(), event.toString()));
                for (MidiListener listener : listeners) {
                    try {
                        listener.onEvent(MidiInDevice.this, event);
                    } catch (RuntimeException e) {
                        log.error("Unexpected error in midi listener", e);
                    }
                }
            }

            @Override
            public void close() {
                stopListening();
            }
        };
        transmitter.setReceiver(receiver);

        // Wait the Java MIDI API is ready to receive messages
        // There is no other way to do this better
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new MidiError(e);
        }
        isListening = true;
    }

    private void closeTransmitter() {
        Optional.ofNullable(transmitter)
                .ifPresent(t -> {
                    t.setReceiver(null);
                    t.close();
                });
        transmitter = null;
    }

    public void stopListening() {
        listeners.clear();
        closeTransmitter();
        isListening = false;
        if (listenTerminated != null) {
            listenTerminated.countDown();
        }
    }

    public void waitNotListening() {
        try {
            if (listenTerminated != null) {
                listenTerminated.await();
            }
        } catch (InterruptedException e) {
            throw new MidiError(e);
        }
    }
}
