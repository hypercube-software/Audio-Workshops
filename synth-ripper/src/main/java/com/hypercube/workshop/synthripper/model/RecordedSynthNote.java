package com.hypercube.workshop.synthripper.model;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import lombok.*;

import java.io.File;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordedSynthNote {
    private String name;
    private LoopSetting loopSetting;
    private int channel;
    private MidiZone velocity;
    private MidiZone ccValue;
    private MidiZone note;
    private int controlChange = MidiPreset.NO_CC;
    private float releaseTimeInSec;
    private MidiPreset preset;
    private File file;
}
