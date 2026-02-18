package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class SysExListener implements MidiListener {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public void onEvent(MidiInDevice device, CustomMidiEvent event) {
        try {
            buffer.write(event.getMessage()
                    .getMessage());
        } catch (IOException e) {
            log.error("Unexpected error", e);
        }
    }

    public int getCurrentSize() {
        return buffer.size();
    }

    public byte[] getSysExResponse() {
        return buffer.toByteArray();
    }
}
