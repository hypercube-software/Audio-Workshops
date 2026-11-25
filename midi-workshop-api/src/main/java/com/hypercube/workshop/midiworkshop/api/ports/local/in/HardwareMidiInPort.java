package com.hypercube.workshop.midiworkshop.api.ports.local.in;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    @Override
    protected void effectiveOpen() throws MidiUnavailableException {
        if (port != null && !port.isOpen()) {
            port.open();
        }
        startListening();
    }

    @Override
    protected void effectiveClose() {
        log.info("Effective close on {}", name);
        stopListening();
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
            private final ByteArrayOutputStream sysExBuffer = new ByteArrayOutputStream();

            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof SysexMessage sysex) {
                    // The OS driver may split a long SysEx into several chunks: only
                    // the frame that ends with F7 is complete. Reassemble here, once,
                    // so every listener always receives a whole frame.
                    byte[] data = sysex.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE
                            ? sysex.getMessage()
                            : sysex.getData();
                    try {
                        sysExBuffer.write(data);
                        if (data[data.length - 1] == (byte) 0xF7) {
                            for (byte[] payload : splitByF0(sysExBuffer.toByteArray())) {
                                dispatch(new SysexMessage(payload, payload.length), timeStamp);
                            }
                            sysExBuffer.reset();
                        }
                    } catch (IOException | InvalidMidiDataException e) {
                        log.error("Unable to reassemble SysEx on {}", getName(), e);
                    }
                    return;
                }
                dispatch(message, timeStamp);
            }

            @Override
            public void close() {
            }
        };
        transmitter.setReceiver(receiver);
    }

    private void dispatch(MidiMessage message, long timeStamp) {
        CustomMidiEvent event = new CustomMidiEvent(message, timeStamp);
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

    /**
     * Split a buffer into the SysEx frames it holds, one per {@code F0} start
     * byte. A single driver chunk can carry several complete frames.
     */
    private static List<byte[]> splitByF0(byte[] data) {
        List<byte[]> messages = new ArrayList<>();
        int lastPos = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == (byte) 0xF0) {
                if (lastPos != -1) {
                    messages.add(Arrays.copyOfRange(data, lastPos, i));
                }
                lastPos = i;
            }
        }
        if (lastPos != -1) {
            messages.add(Arrays.copyOfRange(data, lastPos, data.length));
        }
        return messages;
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
