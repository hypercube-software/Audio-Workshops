package com.hypercube.workshop.midiworkshop.api.presets;

/**
 * @param title name of the drumkit note
 * @param note  MIDI note to use
 */
public record DrumKitNote(String title, int note) {
}
