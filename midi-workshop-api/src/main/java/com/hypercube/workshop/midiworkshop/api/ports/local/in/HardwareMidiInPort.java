package com.hypercube.workshop.midiworkshop.api.ports.local.in;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.util.Optional;

/**
 * As soon as the HardwareMidiInPort is opened, it will listen messages with {@link Transmitter#setReceiver(Receiver)}
 * <ul>
 *     <li>This is hidden in the implementation. </li>
 *     <li>From the outside, you just have to call {@link #addListener(MidiListener)} and {@link #removeListener(MidiListener)}</li>
 *     <li>the listeners list is threadsafe, so you can add or remove listener from multiple threads</li>
 * </ul>
 */
@Slf4j
public class HardwareMidiInPort extends MidiInPort {
    private Transmitter transmitter;
    private MidiDevice port;

    public HardwareMidiInPort(String name) {
        super(name);
    }

    public HardwareMidiInPort(MidiDevice port) {
        super(port.getDeviceInfo()
                .getName());
        this.port = port;
    }


    @Override
    public void open() {
        try {
            if (port != null && !port.isOpen()) {
                port.open();
            }
            super.open();
            startListening();
        } catch (MidiUnavailableException e) {
            throw new MidiError(port, e);
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

    @Override
    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    @Override
    protected void effectiveClose() {
        log.info("Effective close on {}", name);
        port.close();
    }

    protected void setupTransmitter() {
        closeTransmitter();
        try {
            transmitter = port.getTransmitter();
        } catch (MidiUnavailableException e) {
            throw new MidiError(port, e);
        }
        Receiver receiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                CustomMidiEvent event = new CustomMidiEvent(message, timeStamp);
                //log.info("%d listeners, receive %s".formatted(listeners.size(), event.toString()));
                for (MidiListener listener : listeners) {
                    try {
                        Thread.currentThread()
                                .setName("MIDI Listener for '%s'".formatted(getName()));
                        listener.onEvent(HardwareMidiInPort.this, event);
                    } catch (RuntimeException e) {
                        log.error("Unexpected error in midi listener", e);
                    }
                }
            }

            @Override
            public void close() {
            }
        };
        transmitter.setReceiver(receiver);
    }

    @Override
    protected void startListening() {
        if (isListening()) {
            return;
        }
        setupTransmitter();
        super.startListening();
    }

    @Override
    protected void stopListening() {
        closeTransmitter();
        super.stopListening();
    }

    private void closeTransmitter() {
        Optional.ofNullable(transmitter)
                .ifPresent(t -> {
                    Optional.ofNullable(t.getReceiver())
                            .ifPresent(receiver -> {
                                receiver.close();
                                t.setReceiver(null);
                            });
                    t.close();
                });
        transmitter = null;
    }
}
