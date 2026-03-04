package com.hypercube.workshop.midiworkshop.api.clock;

import java.io.Closeable;

public interface MidiClock extends Closeable {
    void start();

    void stop();

    void updateTempo(int tempo);

    void waitNextMidiTick();

    int getResolutionPPQ();

    int MIDI_CLOCK_RESOLUTION_PPQ = 24;
}
