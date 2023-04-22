package com.hypercube.workshop.midiworkshop.common.clock;

import com.hypercube.workshop.midiworkshop.common.seq.TimeSignature;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Compute the position in a song given the time signature and the tick position
 */
@Slf4j
@Getter
public class MidiPosition {
    private static final int DIV_PER_BEAT = 4;
    private final TimeSignature timeSignature;
    private final float quarterPerWhole;
    private final float quarterPerBeat;
    private final float quarterPerBar;
    private final int tickPerQuarter;
    private final int ticksPerBar;
    private final int ticksPerBeat;
    private final int tickPerDiv;
    private final int beatPerBar;

    public MidiPosition(TimeSignature ts) {
        this.timeSignature = ts;
        // if 1 beat = 1/2 => 1/2 whole => 2 quarter
        // if 1 beat = 1/4 => 1/4 whole => 1 quarter
        // if 1 beat = 1/8 => 1/8 whole => 0.5 quarter
        // if 1 beat = 1/16 => 1/16 whole => 0.25 quarter
        quarterPerWhole = 4f;
        quarterPerBeat = quarterPerWhole / ts.denominator();
        quarterPerBar = ts.numerator() * quarterPerBeat;
        tickPerQuarter = MidiClock.MIDI_CLOCK_RESOLUTION_PPQ;
        ticksPerBar = (int) (quarterPerBar * tickPerQuarter);
        ticksPerBeat = (int) (tickPerQuarter * quarterPerBeat);
        tickPerDiv = ticksPerBeat / DIV_PER_BEAT;
        beatPerBar = ts.numerator();
    }

    public void logSettings() {
        log.info(String.format("Time  Signature %s = %d beats of 1/%d", timeSignature, timeSignature.numerator(), timeSignature.denominator()));
        log.info(String.format("Quarter per beat : %f", quarterPerBeat));
        log.info(String.format("Divs per beat    : %d", DIV_PER_BEAT));
        log.info(String.format("Quarter per bar  : %f", quarterPerBar));
        log.info(String.format("Beat    per bar  : %d", beatPerBar));
        log.info(String.format("Ticks per div    : %d", tickPerDiv));
        log.info(String.format("Ticks per quarter: %d", tickPerQuarter));
        log.info(String.format("Ticks per beat   : %d", ticksPerBeat));
        log.info(String.format("Ticks per bar    : %d", ticksPerBar));
    }

    public String getPosition(long currentTickPosition) {
        int barPos = (int) (currentTickPosition / ticksPerBar);
        int beatPos = (int) (currentTickPosition / ticksPerBeat);
        int divPos = (int) (currentTickPosition / tickPerDiv);
        int beat = beatPos % beatPerBar;
        int div = divPos % DIV_PER_BEAT;
        return String.format("%d/%d/%d", barPos + 1, beat + 1, div + 1);
    }
}
