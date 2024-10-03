package com.hypercube.workshop.synthripper.model;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoopSetting {
    private MidiPreset preset;
    private int note;
    private long sampleStart;
    private long sampleEnd;
}
