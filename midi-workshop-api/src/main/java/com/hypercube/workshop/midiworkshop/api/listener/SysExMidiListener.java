package com.hypercube.workshop.midiworkshop.api.listener;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor
public class SysExMidiListener implements MidiListener {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final LinkedBlockingQueue<byte[]> bufferQueue = new LinkedBlockingQueue<>();
    @Getter
    private MidiListener listener;

    public SysExMidiListener(MidiListener listener) {
        this.listener = listener;
    }

    @Override
    public void onEvent(MidiInDevice device, CustomMidiEvent event) {
        if (event.getMessage() instanceof SysexMessage sysexMsg) {
            // This fix an insane bug in Java MIDI API where sysex can be received "split"
            // msg.getMessage() will give you a final F7 whereas it is absolutely not completely received !
            byte[] data = sysexMsg.getStatus() == 0xF0 ? sysexMsg.getMessage() : sysexMsg.getData();
            try {
                buffer.write(data);
                if (data[data.length - 1] == (byte) 0xF7) {
                    byte[] receivedData = buffer.toByteArray();
                    buffer.reset();
                    for (byte[] payload : splitByF0(receivedData)) {
                        String hex = HexFormat.ofDelimiter(" ")
                                .withUpperCase()
                                .formatHex(payload);
                        log.info("Received: {}", hex);
                        bufferQueue.add(payload);
                        if (listener != null) {
                            SysexMessage sysexMessage = new SysexMessage(payload, payload.length);
                            listener.onEvent(device, new CustomMidiEvent(sysexMessage));
                        }
                    }
                } else {
                    log.warn("Receive partial SysEx... Message is bigger than driver buffer, that's fine.");
                }
            } catch (InvalidMidiDataException | IOException e) {
                throw new MidiError(e);
            }
        }
    }

    public int getCurrentSize() {
        return buffer.size();
    }

    public Optional<byte[]> waitResponse(int timeout) {
        try {
            return Optional.ofNullable(bufferQueue.poll(timeout, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<byte[]> splitByF0(byte[] data) {
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
}
