package com.hypercube.workshop.synthripper.model;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
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
    @Builder.Default
    private int controlChange = MidiPreset.NO_CC;
    private float releaseTimeInSec;
    private MidiPreset preset;
    private File file;
    /**
     * Sample position where the note-off occurred (end of the sustain).
     * Recorded at the end of the note capture, used only afterwards, once all
     * wavs have been recorded, by the offline loop detection pass.
     */
    private Long noteOffSampleMarker;
    /**
     * True when the note has a sustain loop (marked during recording)
     */
    @Builder.Default
    private boolean looping = false;
}
