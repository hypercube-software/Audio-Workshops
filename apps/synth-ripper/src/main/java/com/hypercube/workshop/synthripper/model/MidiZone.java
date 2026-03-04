package com.hypercube.workshop.synthripper.model;

/**
 * Express a range for midi values (can be notes, velocity or control change)
 *
 * @param low   greater or equals to 0
 * @param high  lower or equals to 127
 * @param value recorded value for this range
 */
public record MidiZone(int low, int high, int value) {
    @Override
    public String toString() {
        return "[%d,%d,%d]".formatted(low, value, high);
    }

    public int width() {
        return high - low + 1;
    }
}
