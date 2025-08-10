package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.api.MidiInDevice;

import javax.sound.midi.MidiEvent;

public interface MidiMonitorEventListener {
    void onEvent(MidiInDevice device, MidiEvent event);
}
