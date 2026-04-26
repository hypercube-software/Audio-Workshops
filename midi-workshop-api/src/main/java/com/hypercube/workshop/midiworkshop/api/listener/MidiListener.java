package com.hypercube.workshop.midiworkshop.api.listener;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;

@FunctionalInterface
public interface MidiListener {
    void onEvent(MidiInPort device, CustomMidiEvent event);
}
