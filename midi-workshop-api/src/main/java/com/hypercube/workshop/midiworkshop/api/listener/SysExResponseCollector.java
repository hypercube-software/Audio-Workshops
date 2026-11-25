package com.hypercube.workshop.midiworkshop.api.listener;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;

import javax.sound.midi.SysexMessage;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Collects the complete SysEx frames received from a {@link MidiInPort} and
 * lets a single consumer poll them one by one with {@link #waitResponse(int)}.
 *
 * <p>The reassembly of a SysEx split by the OS driver buffer is handled by the
 * input port itself, so this listener only ever sees complete frames. It is
 * meant for request/response protocols that wait for one answer at a time
 * (Sample Dump Standard, a single device query, ...).
 */
public class SysExResponseCollector implements MidiListener {
    private final LinkedBlockingQueue<byte[]> responses = new LinkedBlockingQueue<>();

    @Override
    public void onEvent(MidiInPort device, CustomMidiEvent event) {
        if (event.getMessage() instanceof SysexMessage sysex) {
            responses.add(sysex.getMessage());
        }
    }

    /**
     * Wait for the next complete SysEx frame.
     *
     * @param timeoutSeconds maximum wait in seconds
     * @return the frame bytes ({@code F0 ... F7}), or empty on timeout
     */
    public Optional<byte[]> waitResponse(int timeoutSeconds) {
        try {
            return Optional.ofNullable(responses.poll(timeoutSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MidiError(e);
        }
    }
}
