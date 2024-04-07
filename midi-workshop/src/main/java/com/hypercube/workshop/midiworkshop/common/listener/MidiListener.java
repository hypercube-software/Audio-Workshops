package com.hypercube.workshop.midiworkshop.common.listener;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;

@FunctionalInterface
public interface MidiListener {
    void onEvent(MidiInDevice device, CustomMidiEvent event);
}
