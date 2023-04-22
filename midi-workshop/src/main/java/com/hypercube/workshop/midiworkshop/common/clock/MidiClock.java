package com.hypercube.workshop.midiworkshop.common.clock;

import java.io.Closeable;

public interface MidiClock extends Closeable {
    void start();

    void stop();

    void updateTempo(int tempo);

    void waitNextMidiTick();

    int getResolutionPPQ();

    public static int MIDI_CLOCK_RESOLUTION_PPQ = 24;
}
