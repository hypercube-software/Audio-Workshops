package com.hypercube.workshop.midiworkshop.api.presets;

/**
 * @param title name of the drumkit note
 * @param note  MIDI note to use in [0-127]
 */
public record DrumKitNote(String title, int note) {
}
