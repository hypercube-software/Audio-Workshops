package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import org.jline.utils.Log;

import javax.sound.midi.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class MidiInDevice extends AbstractMidiDevice {
    private final Set<MidiListener> listeners = new HashSet<>();

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
        addListener((device, event) -> {
            if (event.getMessage()
                    .getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
                listener.onEvent(device, event);
            }
        });
    }

    public void removeListener(MidiListener listener) {
        listeners.remove(listener);
    }

    /**
     * Non blocking listen
     *
     * @throws MidiUnavailableException
     */
    public void startListening() throws MidiUnavailableException {
        listenTerminated = new CountDownLatch(1);
        device.getTransmitter()
                .setReceiver(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
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
                });
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
