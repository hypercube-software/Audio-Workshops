package com.hypercube.workshop.midiworkshop.api.listener;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiInDevice;

@FunctionalInterface
public interface MidiListener {
    void onEvent(MidiInDevice device, CustomMidiEvent event);
}
