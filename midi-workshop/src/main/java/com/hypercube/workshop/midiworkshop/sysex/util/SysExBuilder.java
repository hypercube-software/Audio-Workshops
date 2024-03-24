package com.hypercube.workshop.midiworkshop.sysex.util;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import lombok.RequiredArgsConstructor;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Utility class to construct a SysEx ith fluent API style
 */
@RequiredArgsConstructor
public class SysExBuilder {
    private static class State {
        private boolean updateChecksum;
    }

    private final SysExChecksum sysExChecksum;
    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    private final State state = new State();

    public SysExBuilder write(int... values) {
        Arrays.stream(values)
                .forEach(value -> {
                    byteStream.write(value);
                    if (state.updateChecksum) {
                        sysExChecksum.update(value);
                    }

                });
        return this;
    }

    public SysExBuilder beginChecksum() {
        state.updateChecksum = true;
        return this;
    }

    public SysExBuilder writeChecksum() {
        state.updateChecksum = false;
        byteStream.write(sysExChecksum.getValue());
        return this;
    }

    public byte[] buildBuffer() {
        return byteStream.toByteArray();
    }

    /**
     * Forge a MIDI event that can be send to a read MIDI Device
     *
     * @return the MID Event
     * @throws InvalidMidiDataException
     */
    public CustomMidiEvent buildMidiEvent() throws InvalidMidiDataException {
        byte[] data = byteStream.toByteArray();
        return new CustomMidiEvent(new SysexMessage(data, data.length), -1);
    }

}
