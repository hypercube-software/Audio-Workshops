package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.common.listener.SysExMidiListener;
import org.jline.utils.Log;

import javax.sound.midi.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class MidiInDevice extends AbstractMidiDevice {
    private final Set<MidiListener> listeners = ConcurrentHashMap.newKeySet();

    CountDownLatch listenTerminated;

    public MidiInDevice(MidiDevice device) {
        super(device);
    }

    /**
     * Blocking listen
     *
     * @param listener
     * @throws MidiUnavailableException
     */
    public void listen(MidiListener listener) throws MidiUnavailableException {
        addListener(listener);
        startListening();
        waitNotListening();
    }

    public void addListener(MidiListener listener) {
        listeners.add(listener);
    }

    public void addSysExListener(MidiListener listener) {
        addListener(new SysExMidiListener(listener));
    }

    public void removeListener(MidiListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        } else {
            // may be the listener is wrapped in a SysExMidiListener
            listeners.stream()
                    .filter(SysExMidiListener.class::isInstance)
                    .map(l -> (SysExMidiListener) l)
                    .filter(l -> l.listener()
                            .equals(listener))
                    .findFirst()
                    .ifPresent(listeners::remove);
        }

    }

    /**
     * Non blocking listen
     *
     * @throws MidiUnavailableException
     */
    public void startListening() throws MidiUnavailableException {
        listenTerminated = new CountDownLatch(1);
        Transmitter transmitter = device.getTransmitter();
        Receiver receiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                //Log.info("%d listeners, receive %02X".formatted(listeners.size(), message.getStatus()));
                for (MidiListener listener : listeners) {
                    try {
                        listener.onEvent(MidiInDevice.this, new CustomMidiEvent(message, timeStamp));
                    } catch (RuntimeException e) {
                        Log.error("Unexpected error in midi listener", e);
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
    }

    public void stopListening() {
        try {
            listeners.clear();
            var transmitter = device.getTransmitter();
            transmitter.setReceiver(null);
            transmitter.close();
            if (listenTerminated != null) {
                listenTerminated.countDown();
            }
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
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
