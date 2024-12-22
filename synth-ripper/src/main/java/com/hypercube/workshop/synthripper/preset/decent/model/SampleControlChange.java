package com.hypercube.workshop.synthripper.preset.decent.model;

import com.hypercube.workshop.synthripper.model.MidiZone;

public record SampleControlChange(
        int cc,
        MidiZone range) {
}
