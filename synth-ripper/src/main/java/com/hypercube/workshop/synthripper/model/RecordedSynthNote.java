package com.hypercube.workshop.synthripper.model;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class RecordedSynthNote {
    private LoopSetting loopSetting;
    private int lowVelocity;
    private int highVelocity;
    private int velocity;
    private int lowNote;
    private int highNote;
    private int note;
    private float releaseTimeInSec;
    private boolean isFirstVelocity;
    private boolean isFirstNote;
    private MidiPreset preset;
    private File file;
}
