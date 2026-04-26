package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;

import javax.sound.midi.MidiEvent;

public interface MidiMonitorEventListener {
    void onEvent(MidiInPort port, MidiEvent event);
}
