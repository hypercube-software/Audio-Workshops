package com.hypercube.workshop.midiworkshop.common;

@FunctionalInterface
public interface MidiListener {
    void onEvent(MidiInDevice device, CustomMidiEvent event);
}
