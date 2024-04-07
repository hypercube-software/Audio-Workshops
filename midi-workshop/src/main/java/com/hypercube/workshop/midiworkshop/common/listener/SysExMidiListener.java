package com.hypercube.workshop.midiworkshop.common.listener;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;

import javax.sound.midi.SysexMessage;

public record SysExMidiListener(MidiListener listener) implements MidiListener {
    @Override
    public void onEvent(MidiInDevice device, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            listener.onEvent(device, event);
        }
    }
}
